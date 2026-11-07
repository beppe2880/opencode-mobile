package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.integration.AndroidOAuthLoopback
import io.github.beppe2880.opencodemobile.core.data.integration.AndroidOAuthPlatform
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthHttpError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthTransport
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class AndroidOAuthLoopbackTest {
    @Test fun `native OAuth HTTP errors and malformed JSON preserve status without secret response`() = runBlocking {
        listOf(400 to "secret-token", 200 to "not-json secret-token").forEach { (status, body) ->
            val client = OkHttpClient.Builder().addInterceptor {
                Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1).code(status).message("Test")
                    .body(body.toResponseBody()).build()
            }.build()
            val failure = runCatching { AndroidOAuthPlatform(client).json("https://auth.openai.com/test", emptyMap()) }
                .exceptionOrNull() as OAuthHttpError
            assertEquals(status, failure.status)
            assertEquals(null, failure.cause)
            assertTrue(!failure.toString().contains("secret-token"))
        }
    }
    private fun request(port: Int, target: String, method: String = "GET", extra: String = "", host: String = "127.0.0.1:$port"): String =
        Socket("127.0.0.1", port).use { socket ->
            socket.soTimeout = 4000
            socket.getOutputStream().write("$method $target HTTP/1.1\r\nHost: $host\r\n$extra\r\n".toByteArray())
            socket.getInputStream().bufferedReader().readText()
        }

    @Test fun `loopback rejects forged callbacks duplicates origins and wrong paths then accepts exact response`() = runBlocking {
        val callback = AndroidOAuthLoopback("expected-state")
        val port = Url(callback.redirect).port
        try {
            val waiting = async(Dispatchers.IO) { callback.receive().also { callback.respond() } }
            val target = "/auth/callback?code=test-code&client_id=test-client&state=expected-state"
            assertTrue(request(port, target.replace("expected-state", "wrong")).startsWith("HTTP/1.1 400"))
            assertTrue(request(port, "$target&state=expected-state").startsWith("HTTP/1.1 400"))
            assertTrue(request(port, target, method = "POST").startsWith("HTTP/1.1 400"))
            assertTrue(request(port, target, extra = "Origin: https://evil.example\r\n").startsWith("HTTP/1.1 400"))
            assertTrue(request(port, target, host = "evil.example:$port").startsWith("HTTP/1.1 400"))
            assertTrue(request(port, target.replace("/auth/callback", "/other")).startsWith("HTTP/1.1 400"))
            val response = request(port, target)
            assertTrue(response.startsWith("HTTP/1.1 200"))
            assertTrue(response.contains("Cache-Control: no-store"))
            assertTrue(!response.contains("test-code"))
            assertEquals("test-code", withTimeout(5000) { waiting.await() }["code"])
        } finally {
            callback.close()
        }
        ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).use { assertTrue(it.isBound) }
    }

    @Test fun `cancellation releases real listener and correct state denial is received`() = runBlocking {
        val callback = AndroidOAuthLoopback("state")
        val port = Url(callback.redirect).port
        val waiting = async(Dispatchers.IO) {
            try { callback.receive() } finally { callback.close() }
        }
        assertTrue(request(port, "/auth/callback?state=wrong&error=access_denied").startsWith("HTTP/1.1 400"))
        withTimeout(5000) { waiting.cancelAndJoin() }
        ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).use { assertTrue(it.isBound) }
        val denied = AndroidOAuthLoopback("state")
        try {
            val result = async(Dispatchers.IO) { denied.receive().also { denied.respond(ProviderOAuthError(ProviderOAuthStage.Callback)) } }
            assertTrue(request(Url(denied.redirect).port, "/auth/callback?state=state&error=access_denied").startsWith("HTTP/1.1 400"))
            assertEquals("access_denied", withTimeout(5000) { result.await() }["error"])
        } finally { denied.close() }
    }

    @Test fun `browser waits for validation then receives only a safe failure diagnostic`() = runBlocking {
        val callback = AndroidOAuthLoopback("state")
        try {
            val waiting = async(Dispatchers.IO) { callback.receive() }
            val browser = async(Dispatchers.IO) {
                request(Url(callback.redirect).port, "/auth/callback?state=state&code=secret-code&client_id=secret-client")
            }
            assertEquals("secret-code", withTimeout(5000) { waiting.await() }["code"])
            assertFalse(browser.isCompleted)
            callback.respond(ProviderOAuthError(ProviderOAuthStage.TokenExchange, transport = ProviderOAuthTransport.DNS, elapsedMillis = 42))
            val page = withTimeout(5000) { browser.await() }
            assertTrue(page.startsWith("HTTP/1.1 400"))
            assertTrue(page.contains("OAUTH_TokenExchange_DNS_42ms"))
            assertFalse(page.contains("secret-code"))
            assertFalse(page.contains("secret-client"))
            assertTrue(page.contains("Cache-Control: no-store"))
        } finally { callback.close() }
    }

    @Test fun `closing while browser waits releases callback without reporting success`() = runBlocking {
        val callback = AndroidOAuthLoopback("state")
        val waiting = async(Dispatchers.IO) { callback.receive() }
        val browser = async(Dispatchers.IO) {
            request(Url(callback.redirect).port, "/auth/callback?state=state&code=code&client_id=client")
        }
        withTimeout(5000) { waiting.await() }
        assertFalse(browser.isCompleted)
        callback.close()
        assertFalse(withTimeout(5000) { browser.await() }.contains("200 OK"))
    }

    @Test fun `OAuth transport refuses plaintext endpoints before sending anything`() = runBlocking {
        assertTrue(runCatching { AndroidOAuthPlatform().json("http://127.0.0.1/token", emptyMap()) }.isFailure)
    }
}
