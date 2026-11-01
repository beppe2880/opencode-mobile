package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.SttWireFormat
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64

/** One transcription: where, with which key and model, and the WAV file to send. */
class SttRequest(
    /** The provider's API root, e.g. `https://api.openai.com/v1`; `/audio/transcriptions` is appended. */
    val baseUrl: String,
    val format: SttWireFormat,
    /** Sent as a Bearer token; null for a custom server without authentication. */
    val apiKey: String?,
    val model: String,
    val wav: ByteArray,
    /** ISO-639-1 code of the spoken language; null lets the provider detect it. */
    val language: String?,
)

/**
 * Cloud speech-to-text over the two wire formats providers use. It has its own HTTP client:
 * nothing of the OpenCode connections (Basic auth) reaches a provider, and the provider key never
 * reaches an OpenCode server. Keys, audio and transcripts are never logged.
 */
class SpeechToTextClient(
    private val http: HttpClient = defaultClient(),
    private val boundary: () -> String = { "opencode-mobile-" + kotlin.random.Random.nextLong().toULong().toString(16) },
) {

    /** The transcript of [request], trimmed; failures are [TranscriptionError]. */
    suspend fun transcribe(request: SttRequest): String {
        val response = call {
            http.post(transcriptionsUrl(request.baseUrl)) {
                request.apiKey?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                header(HttpHeaders.Accept, "application/json")
                setBody(body(request, boundary()))
            }
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw errorFor(response.status.value, text)
        return parseTranscript(text) ?: throw TranscriptionError.Provider(response.status.value, null)
    }

    /** Models OpenRouter offers for transcription; its model list needs no key. */
    suspend fun openRouterModels(baseUrl: String): List<String> {
        val response = call {
            http.get(baseUrl.trimEnd('/') + "/models") {
                parameter("output_modalities", "transcription")
                header(HttpHeaders.Accept, "application/json")
            }
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw errorFor(response.status.value, text)
        return try {
            Json.parseToJsonElement(text).jsonObject["data"]?.jsonArray.orEmpty().mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
        } catch (_: IllegalArgumentException) {
            throw TranscriptionError.Provider(response.status.value, null)
        }
    }

    private suspend fun call(block: suspend () -> HttpResponse): HttpResponse = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: TranscriptionError) {
        throw e
    } catch (e: Exception) {
        // Only the kind of failure: messages may hold the address.
        logWarning(TAG, "Transcription request failed: ${e::class.simpleName}")
        throw TranscriptionError.Network(e)
    }

    companion object {
        private const val TAG = "SpeechToText"
        private const val DETAIL_MAX = 200
        private val Lenient = Json { ignoreUnknownKeys = true }

        fun transcriptionsUrl(baseUrl: String): String = baseUrl.trimEnd('/') + "/audio/transcriptions"

        /** The request body in [SttRequest.format]; [boundary] separates multipart fields. */
        fun body(request: SttRequest, boundary: String): OutgoingContent = when (request.format) {
            SttWireFormat.OpenRouterJson -> TextContent(openRouterJson(request).toString(), ContentType.Application.Json)
            SttWireFormat.OpenAiMultipart -> MultiPartFormDataContent(
                formData {
                    append("model", request.model)
                    request.language?.let { append("language", it) }
                    append(
                        "file",
                        request.wav,
                        Headers.build {
                            append(HttpHeaders.ContentType, "audio/wav")
                            append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                        },
                    )
                },
                boundary = boundary,
            )
        }

        fun openRouterJson(request: SttRequest): JsonObject = buildJsonObject {
            put("model", request.model)
            putJsonObject("input_audio") {
                put("data", Base64.encode(request.wav))
                put("format", "wav")
            }
            request.language?.let { put("language", it) }
        }

        /** The `text` of a transcription answer; null when there is none. */
        fun parseTranscript(body: String): String? = try {
            (Lenient.parseToJsonElement(body) as? JsonObject)?.get("text")?.let { it as? JsonPrimitive }?.takeIf { it.isString }?.content?.trim()
        } catch (_: IllegalArgumentException) {
            null
        }

        /** The typed failure of an HTTP error answer. */
        fun errorFor(status: Int, body: String): TranscriptionError = when (status) {
            401, 403 -> TranscriptionError.Unauthorized()
            402, 429 -> TranscriptionError.RateLimited()
            413 -> TranscriptionError.TooLarge()
            else -> TranscriptionError.Provider(status, errorDetail(body))
        }

        /**
         * The provider's own short message: `error.message` (OpenAI, Groq, OpenRouter), `message`
         * or `detail` (Mistral, self-hosted servers).
         */
        fun errorDetail(body: String): String? {
            val json = try {
                Lenient.parseToJsonElement(body) as? JsonObject
            } catch (_: IllegalArgumentException) {
                null
            } ?: return null
            val error = json["error"]
            val message = (error as? JsonObject)?.get("message")?.text()
                ?: error?.text()
                ?: json["message"]?.text()
                ?: json["detail"]?.text()
            return message?.trim()?.takeIf { it.isNotEmpty() }?.let { if (it.length > DETAIL_MAX) it.take(DETAIL_MAX) + "…" else it }
        }

        private fun JsonElement.text(): String? = when (this) {
            is JsonPrimitive -> content.takeIf { isString }
            is JsonArray -> firstOrNull()?.let { (it as? JsonObject)?.get("msg")?.text() ?: it.text() }
            is JsonObject -> this["message"]?.text() ?: this["msg"]?.text()
        }

        /**
         * Whether [baseUrl] may receive audio and a key: HTTPS always, plain HTTP only for a
         * server the user typed in ([allowHttp]).
         */
        fun isAllowedBaseUrl(baseUrl: String, allowHttp: Boolean): Boolean {
            val url = try {
                Url(baseUrl.trim())
            } catch (_: Exception) {
                return false
            }
            val scheme = url.protocol.name
            val schemeOk = scheme == "https" || (allowHttp && scheme == "http")
            val prefixOk = baseUrl.trim().startsWith("$scheme://", ignoreCase = true)
            return schemeOk && prefixOk && url.host.isNotBlank()
        }

        private fun defaultClient(): HttpClient = HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
                requestTimeoutMillis = 90_000
            }
        }
    }
}
