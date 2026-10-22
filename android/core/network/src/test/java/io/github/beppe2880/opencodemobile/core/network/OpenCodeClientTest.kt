package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.models.OpenCodeEvent
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class OpenCodeClientTest {

    private val server = MockWebServer()
    private val factory = OpenCodeClientFactory()
    private lateinit var baseUrl: String

    @Before
    fun setUp() {
        server.start()
        baseUrl = server.url("/").toString().trimEnd('/')
    }

    @After
    fun tearDown() = server.shutdown()

    private val info = """{"version":"2.0.27","pid":1,"urls":["http://localhost:4096"],"paths":{"tmp":"/tmp"}}"""

    @Test
    fun `authenticated calls send basic auth for the opencode user`() = runBlocking {
        server.enqueue(MockResponse().setBody(info))
        val client = factory.create(baseUrl, BasicCredentials(password = "s3cret"))

        val result = awaitBody { client.server.serverInfo() }

        assertEquals("2.0.27", result.version)
        val request = server.takeRequest()
        assertEquals("/api/info", request.path)
        assertEquals("Basic b3BlbmNvZGU6czNjcmV0", request.getHeader("Authorization"))
        assertEquals("application/json", request.getHeader("Accept"))
    }

    @Test
    fun `pairing redeems the code without credentials`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"token":"tok_abc"}"""))

        val token = factory.redeemPairing("$baseUrl/", "C0DE")

        assertEquals("tok_abc", token)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/auth/connect/C0DE", request.path)
        assertEquals("application/json", request.getHeader("Accept"))
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `rejected pairing code maps to PairingFailed`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"_tag":"UnauthorizedError","message":"Pairing link expired or already used"}"""),
        )

        val error = failure { factory.redeemPairing(baseUrl, "OLD") }

        assertTrue(error is OpenCodeError.PairingFailed)
        assertEquals("Pairing link expired or already used", error.message)
    }

    @Test
    fun `401 maps to Unauthorized`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Authentication required"}"""))
        val client = factory.create(baseUrl, BasicCredentials(password = "wrong"))

        val error = failure { awaitBody { client.server.serverInfo() } }

        assertTrue(error is OpenCodeError.Unauthorized)
        assertEquals("Authentication required", error.message)
    }

    @Test
    fun `server errors keep tag and message`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"_tag":"InvalidCursorError","message":"Cursor cannot be combined with order"}"""),
        )
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"_tag":"SessionNotFoundError","message":"Session not found: ses_x"}"""))
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>bad gateway</html>"))
        val client = factory.create(baseUrl, BasicCredentials(password = "p"))

        val invalid = failure { awaitBody { client.sessions.sessionGet("ses_x") } } as OpenCodeError.Server
        assertEquals(400, invalid.status)
        assertEquals("InvalidCursorError", invalid.tag)
        assertEquals("Cursor cannot be combined with order", invalid.message)

        assertTrue(failure { awaitBody { client.sessions.sessionGet("ses_x") } } is OpenCodeError.NotFound)

        val gateway = failure { awaitBody { client.sessions.sessionGet("ses_x") } } as OpenCodeError.Server
        assertEquals(502, gateway.status)
        assertNull(gateway.tag)
    }

    @Test
    fun `reading a file asks for its start by path segments and reports the whole size`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(206).setBody("hello")
                .setHeader("Content-Type", "text/plain; charset=utf-8")
                .setHeader("Content-Range", "bytes 0-4/1000"),
        )
        val client = factory.create(baseUrl, BasicCredentials(password = "s3cret"))

        val read = client.readFile("/home/me/app", "src/a b#1.kt", maxBytes = 5)

        assertEquals("hello", read.bytes.decodeToString())
        assertEquals("text/plain", read.mime)
        assertEquals(1000L, read.size)
        val request = server.takeRequest()
        assertEquals("/api/fs/read/src/a%20b%231.kt?location%5Bdirectory%5D=%2Fhome%2Fme%2Fapp", request.path)
        assertEquals("bytes=0-4", request.getHeader("Range"))
        assertEquals("Basic b3BlbmNvZGU6czNjcmV0", request.getHeader("Authorization"))
    }

    @Test
    fun `an empty file reads as no bytes and a missing one as NotFound`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(416))
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"_tag":"FileNotFoundError","message":"File not found: x"}"""))
        val client = factory.create(baseUrl, BasicCredentials(password = "s3cret"))

        val empty = client.readFile("/app", "empty.txt", maxBytes = 10)
        assertEquals(0, empty.bytes.size)
        assertEquals(0L, empty.size)
        try {
            client.readFile("/app", "x", maxBytes = 10)
            fail("expected NotFound")
        } catch (e: OpenCodeError.NotFound) {
            assertEquals("File not found: x", e.message)
        }
    }

    @Test
    fun `connection refused maps to Unreachable`() = runBlocking {
        val closedPort = ServerSocket(0).use { it.localPort }
        val client = factory.create("http://127.0.0.1:$closedPort", BasicCredentials(password = "p"))

        assertTrue(failure { awaitBody { client.server.serverInfo() } } is OpenCodeError.Unreachable)
    }

    @Test
    fun `event stream decodes known events and ends with the response`() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream").setBody(
                ": heartbeat\n\n" +
                    "data: {\"id\":\"e1\",\"type\":\"server.connected\",\"data\":{}}\n\n" +
                    "data: {\"id\":\"e2\",\"created\":1,\"type\":\"future.event\",\"data\":{}}\n\n" +
                    "data: {\"id\":\"e3\",\"created\":2,\"type\":\"session.execution.started\",\"data\":{\"sessionID\":\"ses_1\"}," +
                    "\"durable\":{\"aggregateID\":\"ses_1\",\"seq\":2,\"version\":1}}\n\n",
            ),
        )
        val client = factory.create(baseUrl, BasicCredentials(password = "p"))

        val events = withTimeout(5_000) { client.events().toList() }

        assertEquals(2, events.size)
        assertTrue(events[0] is OpenCodeEvent.ServerconnectedWrapper)
        assertTrue(events[1] is OpenCodeEvent.SessionexecutionstartedWrapper)
        val request = server.takeRequest()
        assertEquals("/api/event", request.path)
        assertEquals("text/event-stream", request.getHeader("Accept"))
        assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun `cancelling the collector closes a stream that has not ended`() = runBlocking {
        val heartbeats = ": heartbeat\n\n".repeat(10_000)
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"id\":\"e1\",\"type\":\"server.connected\",\"data\":{}}\n\n$heartbeats")
                // About 30 minutes to deliver: only cancellation can end this collection in time.
                .throttleBody(64, 1, TimeUnit.SECONDS),
        )
        val client = factory.create(baseUrl, BasicCredentials(password = "p"))

        val first = withTimeout(5_000) { client.events().first() }

        assertTrue(first is OpenCodeEvent.ServerconnectedWrapper)
    }

    @Test
    fun `stream errors are typed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Authentication required"}"""))
        val client = factory.create(baseUrl, BasicCredentials(password = "bad"))

        assertTrue(failure { client.events().toList() } is OpenCodeError.Unauthorized)
    }

    private suspend fun failure(block: suspend () -> Any?): Throwable {
        try {
            block()
        } catch (e: OpenCodeError) {
            return e
        }
        fail("Expected an OpenCodeError")
        error("unreachable")
    }
}
