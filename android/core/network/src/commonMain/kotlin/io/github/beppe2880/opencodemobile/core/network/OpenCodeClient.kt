package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.apis.AgentApi
import io.github.beppe2880.opencodemobile.api.apis.CommandApi
import io.github.beppe2880.opencodemobile.api.apis.ConfigApi
import io.github.beppe2880.opencodemobile.api.apis.FilesystemApi
import io.github.beppe2880.opencodemobile.api.apis.McpApi
import io.github.beppe2880.opencodemobile.api.apis.ModelApi
import io.github.beppe2880.opencodemobile.api.apis.PermissionApi
import io.github.beppe2880.opencodemobile.api.apis.ProjectApi
import io.github.beppe2880.opencodemobile.api.apis.ProviderApi
import io.github.beppe2880.opencodemobile.api.apis.PtyApi
import io.github.beppe2880.opencodemobile.api.apis.ServerApi
import io.github.beppe2880.opencodemobile.api.apis.SessionApi
import io.github.beppe2880.opencodemobile.api.apis.ShellApi
import io.github.beppe2880.opencodemobile.api.apis.VcsApi
import io.github.beppe2880.opencodemobile.api.models.OpenCodeEvent
import io.github.beppe2880.opencodemobile.api.models.OpenCodeEventSerializer
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.exclude
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readBuffer
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Authenticated access to one OpenCode server. Create through [OpenCodeClientFactory]. */
class OpenCodeClient internal constructor(
    val baseUrl: String,
    private val http: HttpClient,
) {
    val server: ServerApi = ServerApi(baseUrl, http)
    val sessions: SessionApi = SessionApi(baseUrl, http)
    val permissions: PermissionApi = PermissionApi(baseUrl, http)
    val agents: AgentApi = AgentApi(baseUrl, http)
    val models: ModelApi = ModelApi(baseUrl, http)
    val providers: ProviderApi = ProviderApi(baseUrl, http)
    val projects: ProjectApi = ProjectApi(baseUrl, http)
    val commands: CommandApi = CommandApi(baseUrl, http)
    val config: ConfigApi = ConfigApi(baseUrl, http)
    val mcps: McpApi = McpApi(baseUrl, http)
    val files: FilesystemApi = FilesystemApi(baseUrl, http)
    val pty: PtyApi = PtyApi(baseUrl, http)
    val shells: ShellApi = ShellApi(baseUrl, http)
    val vcs: VcsApi = VcsApi(baseUrl, http)

    /**
     * Cold WebSocket stream of PTY [ptyId], authenticated like every other call. The server
     * replays its kept output from [cursor] (all of it when null), then sends a
     * [PtyFrame.Cursor] and live output. Each [input] chunk is sent as one binary frame.
     * Pings (see [OpenCodeClientFactory]) detect a dead connection; the PTY may stay silent
     * for hours, so there is no read timeout.
     *
     * Completes when the shell exits, fails with [OpenCodeError.NotFound] when the PTY is gone and
     * with [OpenCodeError] on HTTP or transport errors.
     */
    fun ptyConnect(ptyId: String, directory: String?, cursor: Long?, input: Flow<ByteArray>): Flow<PtyFrame> = channelFlow {
        val url = URLBuilder(baseUrl).apply {
            protocol = if (protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
            appendPathSegments("api", "pty", ptyId, "connect")
            directory?.let { parameters.append("location[directory]", it) }
            cursor?.let { parameters.append("cursor", it.toString()) }
        }.build()
        val frames = this
        // Closing with the error, rather than throwing, delivers the frames already received first.
        val failure = try {
            http.webSocket(
                request = {
                    url(url)
                    exclude(ContentType.Application.Json)
                    timeout {
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    }
                },
            ) {
                val sending = launch { input.collect { send(Frame.Binary(true, it)) } }
                // Read inside the session: the call ends with this block, buffered frames included.
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> frames.send(PtyFrame.Output(frame.readText()))
                        is Frame.Binary -> decodePtyControl(frame.data)?.let { frames.send(it) }
                        else -> Unit
                    }
                }
                sending.cancel()
                val reason = closeReason.await()
                when (val code = reason?.code?.toInt()) {
                    null, NORMAL_CLOSURE -> Unit
                    PTY_GONE -> throw OpenCodeError.NotFound(reason.message.ifEmpty { "Terminal not found" })
                    // Server restarts and proxies: the shell may still be there.
                    else -> throw OpenCodeError.Unreachable("Terminal connection closed ($code)")
                }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenCodeError) {
            e
        } catch (e: WebSocketException) {
            handshakeError(e)
        } catch (e: Exception) {
            e.toOpenCodeError()
        }
        if (failure != null) close(failure)
    }.buffer(Channel.UNLIMITED)

    /**
     * The first [maxBytes] of [path], relative to [directory]. The generated client cannot
     * address `/api/fs/read/{path}` (its path is a wildcard), so the request is built here.
     * Fails with [OpenCodeError.NotFound] when the file does not exist.
     */
    suspend fun readFile(directory: String, path: String, maxBytes: Int): FileBytes {
        val url = URLBuilder(baseUrl).apply {
            appendPathSegments(listOf("api", "fs", "read") + path.split('/').filter { it.isNotEmpty() })
            parameters.append("location[directory]", directory)
        }.build()
        return try {
            http.prepareGet(url) {
                header(HttpHeaders.Range, "bytes=0-${maxBytes - 1}")
                exclude(ContentType.Application.Json)
            }.execute { response -> response.toFileBytes(maxBytes) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenCodeError) {
            throw e
        } catch (e: IOException) {
            throw e.toOpenCodeError()
        }
    }

    private suspend fun HttpResponse.toFileBytes(maxBytes: Int): FileBytes = when {
        // An empty file has no byte to start the range at.
        status.value == RANGE_NOT_SATISFIABLE -> FileBytes(ByteArray(0), null, 0)
        !status.isSuccess() -> throw httpError(status.value, readBounded())
        else -> {
            val bytes = bodyAsChannel().readBuffer(maxBytes.toLong()).readByteArray()
            // `Content-Range: bytes 0-1023/52341` carries the whole size of a partial read.
            val size = headers[HttpHeaders.ContentRange]?.substringAfterLast('/')?.toLongOrNull()
                ?: contentLength()?.takeIf { it >= 0 }
                ?: bytes.size.toLong()
            FileBytes(bytes, headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim(), size)
        }
    }

    /**
     * Cold stream of `/api/event`. Each collection opens one HTTP call that is cancelled with
     * the collector. Unknown event types are skipped. Completes when the server closes the
     * stream and fails with [OpenCodeError] on HTTP or transport errors.
     *
     * The server sends a heartbeat comment every 15 s, so a read that stalls for
     * [readTimeoutMillis] means a dead connection and fails with [OpenCodeError.Unreachable].
     */
    fun events(readTimeoutMillis: Long = STREAM_READ_TIMEOUT_MILLIS): Flow<OpenCodeEvent> = channelFlow {
        try {
            http.prepareGet(baseUrl.trimEnd('/') + "/api/event") {
                header(HttpHeaders.Accept, "text/event-stream")
                exclude(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    socketTimeoutMillis = readTimeoutMillis
                }
            }.execute { response ->
                if (!response.status.isSuccess()) throw httpError(response.status.value, response.readBounded())
                val reader = SseReader(response.bodyAsChannel())
                while (true) {
                    val payload = reader.next() ?: break
                    // `send`, not `emit`: engines such as Darwin run this block in another coroutine.
                    decodeEvent(payload)?.let { send(it) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw e.toOpenCodeError()
        }
    }

    /** Releases this client; the shared engine and its connections stay open for the others. */
    fun close() = http.close()

    companion object {
        /** Three missed server heartbeats (sent every 15 s). */
        const val STREAM_READ_TIMEOUT_MILLIS: Long = 45_000

        private const val NORMAL_CLOSURE = 1000
        private const val RANGE_NOT_SATISFIABLE = 416

        /** Close code the server uses when the PTY does not exist or has exited. */
        private const val PTY_GONE = 4404
    }
}

/** Bytes read from a file of the server; [size] is the whole file, [bytes] may be only its start. */
class FileBytes(val bytes: ByteArray, val mime: String?, val size: Long)

/** One message of the PTY WebSocket. */
sealed interface PtyFrame {
    /** Terminal output; its [String.length] advances the cursor. */
    class Output(val text: String) : PtyFrame

    /** Absolute output position after the replay; resume from it on the next connection. */
    data class Cursor(val value: Long) : PtyFrame
}

/** Binary frames are control frames: a 0x00 byte followed by `{"cursor":N}`. */
internal fun decodePtyControl(bytes: ByteArray): PtyFrame.Cursor? {
    if (bytes.size < 2 || bytes[0] != 0.toByte()) return null
    val json = runCatching { OpenCodeJson.parseToJsonElement(bytes.decodeToString(1, bytes.size)) as? JsonObject }.getOrNull()
    return json?.get("cursor")?.jsonPrimitive?.longOrNull?.let(PtyFrame::Cursor)
}

/**
 * Ktor reports a refused handshake as "... expected status code 101 but was 401"; map the
 * status like any other HTTP error.
 */
private fun handshakeError(e: WebSocketException): OpenCodeError {
    val status = e.message?.let { Regex("""but was (\d{3})""").find(it) }?.groupValues?.get(1)?.toIntOrNull()
    return if (status != null) httpError(status, null) else e.toOpenCodeError()
}

/** Decodes one SSE payload; returns null for event types this client version does not know. */
internal fun decodeEvent(payload: String): OpenCodeEvent? = try {
    OpenCodeJson.decodeFromString(OpenCodeEventSerializer, payload)
} catch (_: IllegalArgumentException) {
    // Also covers SerializationException: unknown `type` discriminators or malformed payloads.
    null
}
