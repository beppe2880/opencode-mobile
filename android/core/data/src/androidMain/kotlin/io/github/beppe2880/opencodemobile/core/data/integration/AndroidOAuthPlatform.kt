package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthTransport
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.math.BigInteger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeSource

/** A separate unauthenticated HTTP client: never forwards OpenCode Basic auth or follows redirects. */
internal class AndroidOAuthPlatform(
    private val http: OkHttpClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).callTimeout(20, TimeUnit.SECONDS).build(),
) : OAuthPlatform {
    override val supported = true
    private val random = SecureRandom()

    override fun randomBytes(): ByteArray = ByteArray(32).also(random::nextBytes)
    override fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
    override fun listen(state: String): OAuthLoopback = AndroidOAuthLoopback(state)

    override suspend fun json(url: String, form: Map<String, String>?): JsonObject {
        require(Url(url).protocol.name == "https")
        val request = Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", "OpenCode-Mobile")
        form?.let { request.post(FormBody.Builder().apply { it.forEach { (key, value) -> add(key, value) } }.build()) }
        return suspendCancellableCoroutine { continuation ->
            val started = TimeSource.Monotonic.markNow()
            val call = http.newCall(request.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(
                        OAuthTransportError(
                            // Only the owning coroutine or OkHttp's call deadline can cancel this private call.
                            // An inactive coroutine is handled above; do not infer timeouts from message text.
                            if (call.isCanceled() && e is InterruptedIOException) ProviderOAuthTransport.Timeout else oauthTransportKind(e),
                            started.elapsedNow().inWholeMilliseconds,
                        ),
                    )
                }
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            if (!it.isSuccessful) throw OAuthHttpError(it.code)
                            val input = it.body.byteStream()
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 262_144)
                                output.write(buffer, 0, count)
                            }
                            Json.parseToJsonElement(output.toString("UTF-8")).jsonObject
                        }
                    }
                    if (continuation.isActive) result.fold(continuation::resume) {
                        val elapsed = started.elapsedNow().inWholeMilliseconds
                        continuation.resumeWithException(
                            if (it is IOException) OAuthTransportError(oauthTransportKind(it), elapsed, response.code)
                            else OAuthHttpError(response.code, elapsed),
                        )
                    }
                }
            })
        }
    }

    override fun verifySignature(token: String, jwks: JsonObject) {
        require(token.length <= 65_536)
        val parts = token.split('.')
        require(parts.size == 3)
        val header = jwtPart(parts[0])
        require(header.string("alg") == "RS256" && header["crit"] == null)
        val kid = header.string("kid")
        val key = requireNotNull(jwks["keys"]).jsonArray.map { it.jsonObject }.single { it.string("kid") == kid }
        require(key.string("kty") == "RSA")
        require(key["use"] == null || key.string("use") == "sig")
        require(key["alg"] == null || key.string("alg") == "RS256")
        val decoder = Base64.getUrlDecoder()
        val modulus = BigInteger(1, decoder.decode(key.string("n")))
        require(modulus.bitLength() >= 2048)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, BigInteger(1, decoder.decode(key.string("e")))))
        require(
            Signature.getInstance("SHA256withRSA").run {
                initVerify(publicKey)
                update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII))
                verify(decoder.decode(parts[2]))
            },
        )
    }
}

/** Bound only to literal IPv4 loopback. Bounded reads and timeouts prevent a local peer pinning the listener. */
internal class AndroidOAuthLoopback(private val state: String) : OAuthLoopback {
    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 500 }
    private val pending = AtomicReference<Socket?>(null)
    override val redirect = "http://127.0.0.1:${server.localPort}/auth/callback"

    override suspend fun receive(): Map<String, String> = withContext(Dispatchers.IO) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val socket = try { server.accept() } catch (_: SocketTimeoutException) { continue }
            var retained = false
            try {
                socket.soTimeout = 1000
                val parsed = try {
                    val input = socket.getInputStream()
                    val bytes = java.io.ByteArrayOutputStream()
                    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
                    var ending = 0
                    while (bytes.size() < 16_384) {
                        currentCoroutineContext().ensureActive()
                        require(System.nanoTime() < deadline)
                        val byte = input.read()
                        require(byte >= 0)
                        bytes.write(byte)
                        ending = (ending shl 8) or byte
                        if (ending == 0x0d0a0d0a) break
                    }
                    require(ending == 0x0d0a0d0a)
                    val lines = bytes.toString("US-ASCII").split("\r\n")
                    require(lines.last() == "")
                    val request = lines.first().split(' ')
                    require(request.size == 3 && request[0] == "GET" && request[1].startsWith("/auth/callback?"))
                    val headers = lines.drop(1).filter { it.contains(':') }.groupBy({ it.substringBefore(':').lowercase() }, { it.substringAfter(':').trim() })
                    require(headers["host"] == listOf("127.0.0.1:${server.localPort}") && headers["origin"] == null)
                    val url = Url("http://127.0.0.1${request[1]}")
                    require(url.encodedPath == "/auth/callback" && url.parameters.entries().all { it.value.size == 1 })
                    val parameters = url.parameters.entries().associate { it.key to it.value.single() }
                    require(parameters["state"] == state)
                    require(parameters["error"] != null || !parameters["code"].isNullOrBlank())
                    parameters
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                if (parsed != null) {
                    pending.set(socket)
                    retained = true
                    return@withContext parsed
                }
                writeResponse(socket, "Invalid authorization response.", false)
            } finally {
                if (!retained) socket.close()
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }

    override suspend fun respond(error: ProviderOAuthError?) = withContext(Dispatchers.IO) {
        pending.getAndSet(null)?.use { socket ->
            val body = if (error == null) {
                "OpenAI authorization verified. Return to OpenCode Mobile to check whether credentials were saved on the server."
            } else {
                "Sign-in failed (${error.diagnosticCode}). Return to OpenCode Mobile to start sign-in again."
            }
            writeResponse(socket, body, error == null)
        }
        Unit
    }

    private fun writeResponse(socket: Socket, body: String, success: Boolean) {
        // The browser may have closed its tab: that must not invalidate an otherwise valid OAuth result.
        runCatching {
            socket.getOutputStream().write(
                (
                    "HTTP/1.1 ${if (success) "200 OK" else "400 Bad Request"}\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\n" +
                        "Content-Security-Policy: default-src 'none'\r\nConnection: close\r\nContent-Length: ${body.length}\r\n\r\n$body"
                    ).toByteArray(Charsets.US_ASCII),
            )
        }
    }

    override fun close() {
        try { server.close() } finally { pending.getAndSet(null)?.close() }
    }
}
