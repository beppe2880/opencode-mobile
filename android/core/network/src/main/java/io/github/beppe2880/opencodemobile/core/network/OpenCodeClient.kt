package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.apis.AgentApi
import io.github.beppe2880.opencodemobile.api.apis.CommandApi
import io.github.beppe2880.opencodemobile.api.apis.FilesystemApi
import io.github.beppe2880.opencodemobile.api.apis.ModelApi
import io.github.beppe2880.opencodemobile.api.apis.PermissionApi
import io.github.beppe2880.opencodemobile.api.apis.ProjectApi
import io.github.beppe2880.opencodemobile.api.apis.ProviderApi
import io.github.beppe2880.opencodemobile.api.apis.PtyApi
import io.github.beppe2880.opencodemobile.api.apis.ServerApi
import io.github.beppe2880.opencodemobile.api.apis.SessionApi
import io.github.beppe2880.opencodemobile.api.models.OpenCodeEvent
import io.github.beppe2880.opencodemobile.api.models.OpenCodeEventSerializer
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/** Authenticated access to one OpenCode server. Create through [OpenCodeClientFactory]. */
class OpenCodeClient internal constructor(
    val baseUrl: String,
    private val http: OkHttpClient,
    retrofit: Retrofit,
) {
    val server: ServerApi = retrofit.create(ServerApi::class.java)
    val sessions: SessionApi = retrofit.create(SessionApi::class.java)
    val permissions: PermissionApi = retrofit.create(PermissionApi::class.java)
    val agents: AgentApi = retrofit.create(AgentApi::class.java)
    val models: ModelApi = retrofit.create(ModelApi::class.java)
    val providers: ProviderApi = retrofit.create(ProviderApi::class.java)
    val projects: ProjectApi = retrofit.create(ProjectApi::class.java)
    val commands: CommandApi = retrofit.create(CommandApi::class.java)
    val files: FilesystemApi = retrofit.create(FilesystemApi::class.java)
    val pty: PtyApi = retrofit.create(PtyApi::class.java)

    private val streamHttp: OkHttpClient by lazy { streamClient(STREAM_READ_TIMEOUT_MILLIS) }

    // Pings detect a dead connection; the PTY may stay silent for hours.
    private val socketHttp: OkHttpClient by lazy {
        http.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(PTY_PING_SECONDS, TimeUnit.SECONDS).build()
    }

    /**
     * Cold WebSocket stream of PTY [ptyId], authenticated like every other call. The server
     * replays its kept output from [cursor] (all of it when null), then sends a
     * [PtyFrame.Cursor] and live output. Each [input] chunk is sent as one binary frame.
     *
     * Completes when the shell exits, fails with [OpenCodeError.NotFound] when the PTY is gone and
     * with [OpenCodeError] on HTTP or transport errors.
     */
    fun ptyConnect(ptyId: String, directory: String?, cursor: Long?, input: Flow<ByteArray>): Flow<PtyFrame> = callbackFlow {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/pty")
            .addPathSegment(ptyId)
            .addPathSegment("connect")
            .apply {
                directory?.let { addQueryParameter("location[directory]", it) }
                cursor?.let { addQueryParameter("cursor", it.toString()) }
            }
            .build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                launch {
                    input.collect { webSocket.send(it.toByteString()) }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(PtyFrame.Output(text))
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                decodePtyControl(bytes)?.let { trySend(it) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE, null)
                when (code) {
                    NORMAL_CLOSURE -> close()
                    PTY_GONE -> close(OpenCodeError.NotFound(reason.ifEmpty { "Terminal not found" }))
                    // Server restarts and proxies: the shell may still be there.
                    else -> close(OpenCodeError.Unreachable("Terminal connection closed ($code)"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(if (response != null && !response.isSuccessful) httpError(response.code, null) else t.toOpenCodeError())
            }
        }
        val socket = socketHttp.newWebSocket(Request.Builder().url(url).build(), listener)
        awaitClose { socket.cancel() }
    }.buffer(Channel.UNLIMITED)

    /**
     * The first [maxBytes] of [path], relative to [directory]. The generated client cannot
     * address `/api/fs/read/{path}` (its path is a wildcard), so the request is built here.
     * Fails with [OpenCodeError.NotFound] when the file does not exist.
     */
    suspend fun readFile(directory: String, path: String, maxBytes: Int): FileBytes {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/fs/read")
            .apply { path.split('/').filter { it.isNotEmpty() }.forEach(::addPathSegment) }
            .addQueryParameter("location[directory]", directory)
            .build()
        val request = Request.Builder().url(url).header("Range", "bytes=0-${maxBytes - 1}").build()
        val response = try {
            http.newCall(request).await()
        } catch (e: IOException) {
            throw e.toOpenCodeError()
        }
        return response.use {
            when {
                // An empty file has no byte to start the range at.
                response.code == RANGE_NOT_SATISFIABLE -> FileBytes(ByteArray(0), null, 0)
                !response.isSuccessful -> throw httpError(response.code, response.body?.readBounded())
                else -> withContext(Dispatchers.IO) {
                    val bytes = try {
                        val source = response.body!!.source()
                        source.request(maxBytes.toLong())
                        source.buffer.readByteArray(minOf(source.buffer.size, maxBytes.toLong()))
                    } catch (e: IOException) {
                        throw e.toOpenCodeError()
                    }
                    // `Content-Range: bytes 0-1023/52341` carries the whole size of a partial read.
                    val size = response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                        ?: response.body?.contentLength()?.takeIf { it >= 0 }
                        ?: bytes.size.toLong()
                    FileBytes(bytes, response.header("Content-Type")?.substringBefore(';')?.trim(), size)
                }
            }
        }
    }

    private fun streamClient(readTimeoutMillis: Long): OkHttpClient =
        http.newBuilder().readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS).build()

    /**
     * Cold stream of `/api/event`. Each collection opens one HTTP call that is cancelled with
     * the collector. Unknown event types are skipped. Completes when the server closes the
     * stream and fails with [OpenCodeError] on HTTP or transport errors.
     *
     * The server sends a heartbeat comment every 15 s, so a read that stalls for
     * [readTimeoutMillis] means a dead connection and fails with [OpenCodeError.Unreachable].
     */
    fun events(readTimeoutMillis: Long = STREAM_READ_TIMEOUT_MILLIS): Flow<OpenCodeEvent> = channelFlow {
        val client = if (readTimeoutMillis == STREAM_READ_TIMEOUT_MILLIS) streamHttp else streamClient(readTimeoutMillis)
        val call = client.newCall(
            Request.Builder().url(baseUrl.trimEnd('/') + "/api/event").header("Accept", "text/event-stream").build(),
        )
        // Reads block, so cancellation has to cancel the call to unblock the reader.
        val reading = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw httpError(response.code, response.body?.readBounded())
                    val reader = SseReader(response.body!!.source())
                    while (true) {
                        val payload = reader.next() ?: break
                        decodeEvent(payload)?.let { send(it) }
                    }
                }
            } catch (e: IOException) {
                if (isActive) throw e.toOpenCodeError()
            }
        }
        try {
            reading.join()
        } finally {
            call.cancel()
        }
    }

    companion object {
        /** Three missed server heartbeats (sent every 15 s). */
        const val STREAM_READ_TIMEOUT_MILLIS: Long = 45_000

        private const val PTY_PING_SECONDS = 20L
        private const val NORMAL_CLOSURE = 1000
        private const val RANGE_NOT_SATISFIABLE = 416

        /** Close code the server uses when the PTY does not exist or has exited. */
        private const val PTY_GONE = 4404
    }
}

/** Bytes read from a file of the server; [size] is the whole file, [bytes] may be only its start. */
class FileBytes(val bytes: ByteArray, val mime: String?, val size: Long)

/** Runs the call on OkHttp's dispatcher; cancelling the coroutine cancels the call. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) = continuation.resume(response) { _, value, _ -> value.close() }
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
        },
    )
}

/** One message of the PTY WebSocket. */
sealed interface PtyFrame {
    /** Terminal output; its [String.length] advances the cursor. */
    class Output(val text: String) : PtyFrame

    /** Absolute output position after the replay; resume from it on the next connection. */
    data class Cursor(val value: Long) : PtyFrame
}

/** Binary frames are control frames: a 0x00 byte followed by `{"cursor":N}`. */
internal fun decodePtyControl(bytes: ByteString): PtyFrame.Cursor? {
    if (bytes.size < 2 || bytes[0] != 0.toByte()) return null
    val json = runCatching { OpenCodeJson.parseToJsonElement(bytes.substring(1).utf8()) as? JsonObject }.getOrNull()
    return json?.get("cursor")?.jsonPrimitive?.longOrNull?.let(PtyFrame::Cursor)
}

/** Decodes one SSE payload; returns null for event types this client version does not know. */
internal fun decodeEvent(payload: String): OpenCodeEvent? = try {
    OpenCodeJson.decodeFromString(OpenCodeEventSerializer, payload)
} catch (_: IllegalArgumentException) {
    // Also covers SerializationException: unknown `type` discriminators or malformed payloads.
    null
}
