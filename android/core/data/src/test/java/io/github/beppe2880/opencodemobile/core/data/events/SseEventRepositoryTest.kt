package io.github.beppe2880.opencodemobile.core.data.events

import io.github.beppe2880.opencodemobile.core.data.TestGraph
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SseEventRepositoryTest {

    private val server = MockWebServer()
    private val scope = CoroutineScope(Job())
    private val visible = MutableStateFlow(true)
    private val networkSignals = MutableSharedFlow<Unit>()
    private lateinit var graph: TestGraph
    private lateinit var repository: SseEventRepository

    @Before
    fun setUp() {
        server.start()
        graph = TestGraph.activeOn(server)
        repository = repository()
    }

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    private fun repository(
        backoff: Backoff = Backoff(initialMillis = 1, maxMillis = 1),
        readTimeoutMillis: Long = OpenCodeClient.STREAM_READ_TIMEOUT_MILLIS,
    ) = SseEventRepository(
        servers = graph.servers,
        clients = graph.clients,
        visibility = object : AppVisibility {
            override val isVisible = visible
        },
        network = object : NetworkMonitor {
            override val networkAvailable = networkSignals
        },
        scope = scope,
        backoff = backoff,
        stopTimeoutMillis = 0,
        readTimeoutMillis = readTimeoutMillis,
    )

    private val connectedEvent = "data: {\"id\":\"e0\",\"type\":\"server.connected\",\"data\":{}}\n\n"

    private fun stream(vararg sessionIds: String) = MockResponse()
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
            connectedEvent +
                sessionIds.joinToString("") { id ->
                    "data: {\"id\":\"e_$id\",\"created\":1,\"type\":\"session.execution.started\"," +
                        "\"data\":{\"sessionID\":\"$id\"},\"durable\":{\"aggregateID\":\"$id\",\"seq\":1,\"version\":1}}\n\n"
                },
        )

    @Test
    fun `announces every reconnect and maps events`() = runBlocking {
        server.enqueue(stream("ses_1"))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(stream("ses_2"))

        val events = withTimeout(5_000) { repository.events.take(4).toList() }

        assertEquals(
            listOf(
                ServerEvent.Connected,
                ServerEvent.ExecutionStarted("ses_1"),
                ServerEvent.Connected,
                ServerEvent.ExecutionStarted("ses_2"),
            ),
            events,
        )
        assertEquals(3, server.requestCount)
    }

    /** A stream that delivers `server.connected` and then stays open for a long time. */
    private fun openStream() = MockResponse()
        .setHeader("Content-Type", "text/event-stream")
        .setBody(connectedEvent + ": heartbeat\n\n".repeat(10_000))
        .throttleBody(256, 1, TimeUnit.SECONDS)

    @Test
    fun `pauses while the app is in the background`() = runBlocking {
        visible.value = false
        server.enqueue(openStream())
        val collecting = repository.events.launchIn(scope)

        assertEquals(ConnectionState.Disconnected, repository.connection.value)

        visible.value = true
        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Connected } }
        assertEquals(1, server.requestCount)

        visible.value = false
        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Disconnected } }
        collecting.cancel()
    }

    @Test
    fun `stays connected in the background while kept alive`() = runBlocking {
        server.enqueue(openStream())
        val collecting = repository.events.launchIn(scope)
        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Connected } }

        repository.keepAliveInBackground(true)
        visible.value = false
        delay(300)
        assertEquals(ConnectionState.Connected, repository.connection.value)
        assertEquals(1, server.requestCount)

        repository.keepAliveInBackground(false)
        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Disconnected } }
        collecting.cancel()
    }

    @Test
    fun `disconnects when the last collector leaves`(): Unit = runBlocking {
        server.enqueue(openStream())
        val collecting = repository.events.launchIn(scope)
        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Connected } }

        collecting.cancel()

        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Disconnected } }
    }

    @Test
    fun `stops retrying when the credentials are rejected`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(stream("ses_1"))
        val collecting = repository.events.launchIn(scope)

        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Unauthorized } }

        assertNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        // With a 1 ms backoff a retry would arrive well within this window.
        assertNull(server.takeRequest(300, TimeUnit.MILLISECONDS))
        assertEquals(1, server.requestCount)
        assertEquals(ConnectionState.Unauthorized, repository.connection.value)
        collecting.cancel()
    }

    @Test
    fun `reconnects when the stream stalls`() = runBlocking {
        repository = repository(readTimeoutMillis = 300)
        // Sends `server.connected`, then nothing (not even heartbeats) for longer than the read timeout.
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(connectedEvent + ": heartbeat\n\n")
                .throttleBody(connectedEvent.length.toLong(), 2, TimeUnit.SECONDS),
        )
        server.enqueue(stream("ses_1"))

        val events = withTimeout(5_000) { repository.events.take(3).toList() }

        assertEquals(
            listOf(ServerEvent.Connected, ServerEvent.Connected, ServerEvent.ExecutionStarted("ses_1")),
            events,
        )
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `network becoming available cuts the reconnect wait`() = runBlocking {
        repository = repository(backoff = Backoff(initialMillis = 60_000, maxMillis = 60_000))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(stream("ses_1"))
        val connected = scope.async { repository.events.first() }

        withTimeout(5_000) { repository.connection.first { it == ConnectionState.Reconnecting } }
        withTimeout(5_000) { networkSignals.subscriptionCount.first { it > 0 } }
        networkSignals.emit(Unit)

        // Without the signal the next attempt would wait at least 30 s.
        assertEquals(ServerEvent.Connected, withTimeout(5_000) { connected.await() })
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `backoff grows, is capped and jittered within half of the delay`() {
        val backoff = Backoff(initialMillis = 100, maxMillis = 1_000, random = Random(42))
        repeat(50) {
            assertTrue(backoff.delayMillis(0) in 50L..100L)
            assertTrue(backoff.delayMillis(2) in 200L..400L)
            assertTrue(backoff.delayMillis(30) in 500L..1_000L)
        }
    }
}
