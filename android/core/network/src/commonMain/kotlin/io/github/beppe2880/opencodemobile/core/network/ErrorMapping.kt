package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.infrastructure.HttpResponse
import io.github.beppe2880.opencodemobile.api.models.SessionMessagesResponse
import io.github.beppe2880.opencodemobile.api.models.SessionsResponse
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.io.readString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private const val MAX_ERROR_BODY = 16 * 1024

/**
 * Awaits a generated API call and returns its body, throwing [OpenCodeError] on any failure.
 * The body is decoded here rather than by content negotiation, so a response without a JSON
 * `Content-Type` still decodes; message and session pages are decoded by [TolerantPages].
 */
suspend inline fun <reified T : Any> awaitBody(noinline call: suspend () -> HttpResponse<T>): T =
    decodeBody(awaitResponse(call).response, typeOf<T>())

/** Awaits a generated API call whose body is irrelevant, throwing [OpenCodeError] on any failure. */
suspend fun <T : Any> awaitSuccess(call: suspend () -> HttpResponse<T>) {
    awaitResponse(call)
}

@PublishedApi
internal suspend fun <T : Any> awaitResponse(call: suspend () -> HttpResponse<T>): HttpResponse<T> {
    val response = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toOpenCodeError()
    }
    if (!response.success) throw httpError(response.status, response.response.readBounded())
    return response
}

@PublishedApi
internal suspend fun <T> decodeBody(response: io.ktor.client.statement.HttpResponse, type: KType): T = try {
    val text = response.bodyAsText()
    @Suppress("UNCHECKED_CAST")
    when (type.classifier) {
        SessionMessagesResponse::class -> TolerantPages.messages(OpenCodeJson, text) as T
        SessionsResponse::class -> TolerantPages.sessions(OpenCodeJson, text) as T
        else -> OpenCodeJson.decodeFromString(OpenCodeJson.serializersModule.serializer(type), text) as T
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw e.toOpenCodeError()
}

/** Reads at most [MAX_ERROR_BODY] bytes of an error body. */
internal suspend fun io.ktor.client.statement.HttpResponse.readBounded(): String? = try {
    bodyAsChannel().readBuffer(MAX_ERROR_BODY).readString()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

/** Maps transport and decoding failures; [OpenCodeError]s pass through unchanged. */
fun Throwable.toOpenCodeError(): OpenCodeError = when (this) {
    is OpenCodeError -> this
    is IOException -> OpenCodeError.Unreachable(message ?: this::class.simpleName.orEmpty(), this)
    is SerializationException ->
        OpenCodeError.Server(status = 0, tag = null, message = "Unexpected server response: $message")
    else -> OpenCodeError.Server(status = 0, tag = null, message = message ?: this::class.simpleName.orEmpty())
}

/**
 * True when [OpenCodeError.Unreachable] comes from a read or request timeout rather than a lost
 * connection. A timeout while reading the body can arrive wrapped (e.g. in a closed channel).
 */
val OpenCodeError.isTimeout: Boolean
    get() = this is OpenCodeError.Unreachable &&
        generateSequence(cause) { it.cause }.take(8).any { it is SocketTimeoutException || it is HttpRequestTimeoutException }

/** Maps a non-2xx response with an optional `{ "_tag", "message" }` JSON body. */
internal fun httpError(status: Int, body: String?): OpenCodeError {
    val (tag, message) = parseErrorBody(body)
    return when (status) {
        401 -> OpenCodeError.Unauthorized(message ?: "Authentication failed", httpStatus = status)
        404 -> OpenCodeError.NotFound(message ?: "Not found", httpStatus = status)
        else -> OpenCodeError.Server(status, tag, message ?: "HTTP $status")
    }
}

internal fun pairingError(error: OpenCodeError): OpenCodeError = when (error) {
    is OpenCodeError.Unauthorized -> OpenCodeError.PairingFailed(error.message.orEmpty())
    is OpenCodeError.NotFound -> OpenCodeError.PairingFailed(error.message.orEmpty())
    is OpenCodeError.Server -> if (error.status in 400..499) OpenCodeError.PairingFailed(error.message.orEmpty()) else error
    else -> error
}

private fun parseErrorBody(body: String?): Pair<String?, String?> {
    if (body.isNullOrBlank()) return null to null
    val json = runCatching { OpenCodeJson.parseToJsonElement(body) as? JsonObject }.getOrNull()
        ?: return null to null
    fun field(name: String) = runCatching { json[name]?.jsonPrimitive?.contentOrNull }.getOrNull()
    return field("_tag") to field("message")
}
