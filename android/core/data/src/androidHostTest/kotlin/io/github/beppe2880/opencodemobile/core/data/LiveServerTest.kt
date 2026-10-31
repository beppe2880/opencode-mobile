package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.cache.InMemoryMessageCache
import io.github.beppe2880.opencodemobile.core.data.catalog.RemoteCatalogRepository
import io.github.beppe2880.opencodemobile.core.data.chat.RemoteChatRepository
import io.github.beppe2880.opencodemobile.core.data.events.AppVisibility
import io.github.beppe2880.opencodemobile.core.data.events.NetworkMonitor
import io.github.beppe2880.opencodemobile.core.data.events.SseEventRepository
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.network.BasicCredentials
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Smoke test against a real server; skipped unless `OPENCODE_LIVE_PASSWORD` is set.
 * `OPENCODE_LIVE_URL` defaults to http://localhost:4096. The prompt round trip only runs in a
 * scratch directory (`OPENCODE_LIVE_DIR`, a temporary one by default) with the cheap Haiku
 * model, and the session is deleted afterwards.
 */
class LiveServerTest {

    private val password: String? = System.getenv("OPENCODE_LIVE_PASSWORD")
    private val url = System.getenv("OPENCODE_LIVE_URL") ?: "http://localhost:4096"
    private val scratch = System.getenv("OPENCODE_LIVE_DIR") ?: (System.getProperty("java.io.tmpdir") + "/opencode-mobile-live")
    private val scope = CoroutineScope(Job())

    @Before
    fun requireServer() = assumeTrue("OPENCODE_LIVE_PASSWORD not set", password != null)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `password and pairing flows`() = runBlocking {
        val graph = TestGraph(FakeServerStore())
        val servers = graph.servers

        assertTrue(servers.probe(url, password).getOrThrow().version.isNotEmpty())
        assertTrue(servers.probe(url, "definitely-wrong").exceptionOrNull() is OpenCodeError.Unauthorized)
        assertTrue(servers.pair(url, "nonexistent-code").exceptionOrNull() is OpenCodeError.PairingFailed)

        val authed = graph.factory.create(url, BasicCredentials(password = password!!))
        val code = awaitBody { authed.server.serverPair() }.code
        val paired = servers.pair(url, code).getOrThrow()
        assertEquals(CredentialKind.PairingToken, paired.credential)
        assertTrue(servers.pair(url, code).exceptionOrNull() is OpenCodeError.PairingFailed)

        val sessions = RemoteSessionRepository(graph.clients)
        assertTrue(sessions.list(limit = 5).getOrThrow().all { it.parentId == null })
        sessions.active().getOrThrow()
        sessions.projects().getOrThrow()
        val catalog = RemoteCatalogRepository(graph.clients)
        assertTrue(catalog.agents(scratch).getOrThrow().any { it.id == "build" })
        assertTrue(catalog.models(scratch).getOrThrow().isNotEmpty())
    }

    @Test
    fun `prompt round trip streams events and stores the answer`() = runBlocking {
        File(scratch).mkdirs()
        val graph = TestGraph(FakeServerStore())
        graph.servers.addWithPassword(url, password!!).getOrThrow()
        val events = SseEventRepository(
            graph.servers,
            graph.clients,
            object : AppVisibility {
                override val isVisible = MutableStateFlow(true)
            },
            object : NetworkMonitor {
                override val connected = emptyFlow<Boolean>()
            },
            scope,
        )
        val sessions = RemoteSessionRepository(graph.clients)
        val chat = RemoteChatRepository(graph.clients, InMemoryMessageCache())

        val connected = async { withTimeout(10_000) { events.events.first { it == ServerEvent.Connected } } }
        connected.await()
        val session = sessions.create(scratch, agent = "build", model = ModelRef("claude-haiku-5-5", "anthropic")).getOrThrow()
        try {
            val ended = async {
                withTimeout(120_000) {
                    events.events.first { it is ServerEvent.ExecutionEnded && it.sessionId == session.id }
                }
            }
            sessions.rename(session.id, "ocm live smoke").getOrThrow()
            chat.prompt(session.id, "Reply with exactly the word OK and nothing else.").getOrThrow()
            ended.await()

            val transcript = chat.messages(session.id).getOrThrow()
            assertTrue(transcript.first() is Message.User)
            val answer = transcript.filterIsInstance<Message.Assistant>().flatMap { it.parts }.filterIsInstance<Part.Text>()
            assertTrue(answer.joinToString { it.text }.contains("OK"))
            assertEquals("ocm live smoke", sessions.get(session.id).getOrThrow().title)
            assertTrue(chat.permissions(session.id).getOrThrow().isEmpty())
        } finally {
            sessions.delete(session.id).getOrThrow()
        }
    }
}
