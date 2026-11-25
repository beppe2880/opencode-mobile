package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class SessionExportDownloadTest {
    private val server = MockWebServer()
    private lateinit var client: OpenCodeClient

    @Before
    fun setUp() {
        server.start()
        client = OpenCodeClientFactory().create(server.url("/").toString().trimEnd('/'), BasicCredentials(password = "fixture"))
    }

    @After
    fun tearDown() {
        client.close()
        server.shutdown()
    }

    private fun body(size: Int) = """{"data":{"info":{},"messages":[]},"extra":"${"x".repeat(size)}"}"""

    @Test
    fun `download reports actual bytes before decoding with a known total`() = runBlocking {
        val body = body(128 * 1024)
        val bytes = body.encodeToByteArray().size.toLong()
        server.enqueue(MockResponse().setBody(body).throttleBody(8192, 10, TimeUnit.MILLISECONDS))
        val progress = mutableListOf<Pair<Long, Long?>>()
        var downloaded = false
        val result = client.downloadSessionExport(
            "chat",
            onDownload = { count, total ->
                assertTrue(!downloaded)
                progress += count to total
            },
            onDownloaded = {
                assertEquals(bytes, progress.last().first)
                downloaded = true
            },
        )
        assertEquals(Json.parseToJsonElement(body), result)
        assertTrue(progress.any { it.first in 1 until bytes })
        assertTrue(progress.all { it.second == bytes })
        assertTrue(progress.zipWithNext().all { (a, b) -> a.first <= b.first })
        assertEquals("/api/experimental/session/chat/export?sanitize=false", server.takeRequest().path)
    }

    @Test
    fun `unknown chunked length reports bytes without inventing a total`() = runBlocking {
        val body = body(16 * 1024)
        server.enqueue(MockResponse().setChunkedBody(body, 1024))
        val progress = mutableListOf<Pair<Long, Long?>>()
        client.downloadSessionExport("chat", { count, total -> progress += count to total }, {})
        assertTrue(progress.isNotEmpty())
        assertTrue(progress.all { it.second == null })
        assertEquals(body.encodeToByteArray().size.toLong(), progress.last().first)
    }

    @Test
    fun `simultaneous exports keep listeners isolated`() = runBlocking {
        val a = body(16 * 1024)
        val b = body(32 * 1024)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setBody(if (request.path.orEmpty().contains("/a/")) a else b)
        }
        val first = mutableListOf<Long?>()
        val second = mutableListOf<Long?>()
        coroutineScope {
            val one = async { client.downloadSessionExport("a", { _, total -> first += total }, {}) }
            val two = async { client.downloadSessionExport("b", { _, total -> second += total }, {}) }
            one.await()
            two.await()
        }
        assertTrue(first.isNotEmpty() && second.isNotEmpty())
        assertTrue(first.all { it == a.encodeToByteArray().size.toLong() })
        assertTrue(second.all { it == b.encodeToByteArray().size.toLong() })
    }

    @Test
    fun `cancellation propagates and an HTTP error does not break later exports`() = runBlocking {
        server.enqueue(MockResponse().setBody(body(1024)))
        var cancelled = false
        try {
            client.downloadSessionExport("chat", { _, _ -> throw CancellationException("cancelled") }, {})
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        server.enqueue(MockResponse().setResponseCode(404))
        var notFound = false
        try {
            client.downloadSessionExport("chat", { _, _ -> }, {})
        } catch (_: OpenCodeError.NotFound) {
            notFound = true
        }
        assertTrue(notFound)
        val body = body(10)
        server.enqueue(MockResponse().setBody(body))
        assertEquals(Json.parseToJsonElement(body), client.downloadSessionExport("chat", { _, _ -> }, {}))
    }
}
