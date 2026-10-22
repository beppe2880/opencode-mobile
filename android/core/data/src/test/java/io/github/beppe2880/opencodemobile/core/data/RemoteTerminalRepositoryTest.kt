package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.terminal.RemoteTerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.domain.TerminalUpdate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class RemoteTerminalRepositoryTest {

    private val server = MockWebServer()
    private lateinit var terminals: RemoteTerminalRepository

    @Before
    fun setUp() {
        server.start()
        terminals = RemoteTerminalRepository(TestGraph.activeOn(server).clients, retryDelaysMillis = listOf(0))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun cursorFrame(cursor: Long): ByteString = (byteArrayOf(0) + """{"cursor":$cursor}""".encodeToByteArray()).toByteString()

    private fun socket(onOpen: (WebSocket) -> Unit, onBytes: (WebSocket, ByteString) -> Unit = { _, _ -> }) = MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = onOpen(webSocket)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = onBytes(webSocket, bytes)
        },
    )

    private fun List<TerminalUpdate>.describe() = map {
        when (it) {
            is TerminalUpdate.Output -> "out:${it.text}"
            is TerminalUpdate.Link -> it.state.name
        }
    }

    @Test
    fun `a dropped connection resumes from the cursor instead of replaying the output again`() = runBlocking {
        server.enqueue(
            socket({ ws ->
                ws.send("hello")
                ws.send(cursorFrame(5))
                ws.send("abc")
                ws.close(1001, "server restart")
            }),
        )
        server.enqueue(
            socket({ ws ->
                ws.send(cursorFrame(8))
                ws.send("xyz")
                ws.close(1000, null)
            }),
        )

        val updates = withTimeout(10_000) { terminals.attach("/work", "pty_1", emptyFlow()).toList() }

        assertEquals(
            listOf("Connecting", "out:hello", "Connected", "out:abc", "Reconnecting", "Connected", "out:xyz", "Exited"),
            updates.describe(),
        )
        assertEquals("/api/pty/pty_1/connect?location%5Bdirectory%5D=%2Fwork", server.takeRequest().path)
        assertEquals("/api/pty/pty_1/connect?location%5Bdirectory%5D=%2Fwork&cursor=8", server.takeRequest().path)
    }

    @Test
    fun `typed input reaches the server as binary frames with the saved credentials`() = runBlocking {
        val received = LinkedBlockingQueue<String>()
        server.enqueue(
            socket(
                onOpen = { ws -> ws.send(cursorFrame(0)) },
                onBytes = { ws, bytes ->
                    received += bytes.utf8()
                    if (bytes.utf8() == "\r") ws.close(1000, null)
                },
            ),
        )

        val updates = withTimeout(10_000) {
            terminals.attach("/work", "pty_1", flowOf("ls".encodeToByteArray(), "\r".encodeToByteArray())).toList()
        }

        assertEquals(listOf("ls", "\r"), listOf(received.poll(5, TimeUnit.SECONDS), received.poll(5, TimeUnit.SECONDS)))
        assertEquals(listOf("Connecting", "Connected", "Exited"), updates.describe())
        assertTrue(server.takeRequest().getHeader("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun `a terminal that is gone ends without retrying`() = runBlocking {
        server.enqueue(socket({ ws -> ws.close(4404, "session exited") }))

        val updates = withTimeout(10_000) { terminals.attach("/work", "pty_1", emptyFlow()).toList() }

        assertEquals(listOf("Connecting", "Exited"), updates.describe())
        assertEquals(1, server.requestCount)
    }

    @Test(expected = OpenCodeError.Unauthorized::class)
    fun `rejected credentials stop the terminal`(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        terminals.attach("/work", "pty_1", emptyFlow()).toList()
    }
}
