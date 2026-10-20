package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.cache.CachedEntry
import io.github.beppe2880.opencodemobile.core.data.cache.CachedTranscript
import io.github.beppe2880.opencodemobile.core.data.cache.InMemoryMessageCache
import io.github.beppe2880.opencodemobile.core.data.catalog.RemoteCatalogRepository
import io.github.beppe2880.opencodemobile.core.data.chat.RemoteChatRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServerRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteRepositoriesTest {

    private val server = MockWebServer()
    private lateinit var graph: TestGraph

    @Before
    fun setUp() {
        server.start()
        graph = TestGraph.activeOn(server)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String) = requireNotNull(javaClass.classLoader?.getResource(name)).readText()

    private fun userMessage(index: Int) =
        """{"id":"msg_$index","time":{"created":$index},"type":"user","text":"prompt $index"}"""

    @Test
    fun `a new session loads its newest page first and older pages on demand`() = runBlocking {
        val chat = RemoteChatRepository(graph.clients, InMemoryMessageCache())
        val newest = (254 downTo 205).joinToString(",") { userMessage(it) }
        val middle = (204 downTo 5).joinToString(",") { userMessage(it) }
        val oldest = listOf(
            userMessage(4),
            """{"id":"msg_sys","time":{"created":3},"type":"system","text":"ctx"}""",
            userMessage(2),
        ).joinToString(",")
        server.enqueue(MockResponse().setBody("""{"data":[$newest],"cursor":{"next":"c1"}}"""))
        server.enqueue(MockResponse().setBody("""{"data":[$middle],"cursor":{"next":"c2"}}"""))
        server.enqueue(MockResponse().setBody("""{"data":[$oldest],"cursor":{"next":"c3"}}"""))

        val first = chat.messages("ses_1").getOrThrow()
        assertEquals("msg_205", first.first().id)
        assertEquals("msg_254", first.last().id)
        assertEquals("/api/session/ses_1/message?limit=50", server.takeRequest().path)

        val middlePage = chat.olderMessages("ses_1").getOrThrow()!!
        assertEquals(listOf("msg_5", "msg_204"), listOf(middlePage.first().id, middlePage.last().id))
        assertEquals("/api/session/ses_1/message?limit=200&cursor=c1", server.takeRequest().path)

        assertEquals(listOf("msg_2", "msg_4"), chat.olderMessages("ses_1").getOrThrow()!!.map { it.id })
        assertEquals("/api/session/ses_1/message?limit=200&cursor=c2", server.takeRequest().path)
        // The last page was short, so the history is complete.
        assertEquals(null, chat.olderMessages("ses_1").getOrThrow())
        assertEquals(3, server.requestCount)

        val cached = chat.cachedMessages("ses_1")
        assertEquals(252, cached.size)
        assertEquals(listOf("msg_2", "msg_254"), listOf(cached.first().id, cached.last().id))
        assertTrue(cached.all { it is Message.User })
    }

    @Test
    fun `history beyond the cache cap is still loaded but not stored`() = runBlocking {
        val chat = RemoteChatRepository(graph.clients, InMemoryMessageCache(), maxMessages = 150)
        server.enqueue(page(*(250 downTo 201).map(::userMessage).toTypedArray(), next = "c1"))
        server.enqueue(page(*(200 downTo 1).map(::userMessage).toTypedArray(), next = "c2"))
        server.enqueue(page(userMessage(0), next = "c3"))

        chat.messages("ses_1").getOrThrow()
        // The whole page is shown even though only 100 entries fit in the cache.
        assertEquals(200, chat.olderMessages("ses_1").getOrThrow()!!.size)
        assertEquals(listOf("msg_0"), chat.olderMessages("ses_1").getOrThrow()!!.map { it.id })
        repeat(2) { server.takeRequest() }
        assertEquals("/api/session/ses_1/message?limit=200&cursor=c2", server.takeRequest().path)
        assertEquals(null, chat.olderMessages("ses_1").getOrThrow())
        assertEquals(3, server.requestCount)

        val cached = chat.cachedMessages("ses_1")
        assertEquals(150, cached.size)
        assertEquals("msg_101", cached.first().id)
    }

    @Test
    fun `older history is found again after a sync trimmed the cache`() = runBlocking {
        val cache = InMemoryMessageCache()
        val chat = RemoteChatRepository(graph.clients, cache, maxMessages = 3)
        server.enqueue(page(userMessage(3), userMessage(2), userMessage(1)))
        chat.messages("ses_1").getOrThrow()
        // Two new messages push the oldest two out of the capped cache, and its cursor with them.
        server.enqueue(page(userMessage(5), userMessage(4), userMessage(3), next = "c1"))
        assertEquals(listOf("msg_3", "msg_4", "msg_5"), chat.messages("ses_1").getOrThrow().map { it.id })
        assertEquals(null, cache.sessions["s1" to "ses_1"]?.olderCursor)
        repeat(2) { server.takeRequest() }

        server.enqueue(page(userMessage(5), userMessage(4), userMessage(3), userMessage(2), userMessage(1), next = "c2"))
        assertEquals(listOf("msg_1", "msg_2"), chat.olderMessages("ses_1").getOrThrow()!!.map { it.id })
        assertEquals("/api/session/ses_1/message?limit=200", server.takeRequest().path)
        // That page was short: the history is complete.
        assertEquals(null, chat.olderMessages("ses_1").getOrThrow())
        assertEquals(3, server.requestCount)
        assertEquals(listOf("msg_3", "msg_4", "msg_5"), chat.cachedMessages("ses_1").map { it.id })
    }

    @Test
    fun `undecodable messages are skipped without ending the history`() = runBlocking {
        val chat = RemoteChatRepository(graph.clients, InMemoryMessageCache())
        val newest = (51 downTo 3).map(::userMessage) + """{"id":"msg_x","type":"from-the-future"}"""
        server.enqueue(page(*newest.toTypedArray(), next = "c1"))
        server.enqueue(page(userMessage(2), userMessage(1)))

        assertEquals(49, chat.messages("ses_1").getOrThrow().size)
        // The page counted 50 entries, so older history is still requested.
        assertEquals(listOf("msg_1", "msg_2"), chat.olderMessages("ses_1").getOrThrow()!!.map { it.id })
    }

    @Test
    fun `prefetch stores the last messages of sessions not cached yet`() = runBlocking {
        val cache = InMemoryMessageCache()
        val chat = RemoteChatRepository(graph.clients, cache)
        cache.sessions["s1" to "ses_cached"] = CachedTranscript(listOf(CachedEntry("msg_1", userMessage(1))))
        server.enqueue(page(userMessage(9), userMessage(8), next = "c1"))

        chat.prefetch(listOf("ses_cached", "ses_new"), lastMessages = 2)

        assertEquals("/api/session/ses_new/message?limit=2", server.takeRequest().path)
        assertEquals(1, server.requestCount)
        assertEquals(listOf("msg_8", "msg_9"), chat.cachedMessages("ses_new").map { it.id })
        assertEquals("c1", cache.sessions["s1" to "ses_new"]?.olderCursor)
    }

    @Test
    fun `prefetch tops up sessions cached with fewer messages than asked`() = runBlocking {
        val cache = InMemoryMessageCache()
        val chat = RemoteChatRepository(graph.clients, cache)
        cache.sessions["s1" to "ses_short"] = CachedTranscript(listOf(CachedEntry("msg_8", userMessage(8))), olderCursor = "c0")
        server.enqueue(page(userMessage(9), userMessage(8), userMessage(7), next = "c1"))

        chat.prefetch(listOf("ses_short"), lastMessages = 3)

        assertEquals("/api/session/ses_short/message?limit=3", server.takeRequest().path)
        assertEquals(listOf("msg_7", "msg_8", "msg_9"), chat.cachedMessages("ses_short").map { it.id })
    }

    @Test
    fun `the catalog is fetched once and reused until it expires`() = runBlocking {
        var now = 0L
        val catalog = RemoteCatalogRepository(graph.clients, clock = { now })
        repeat(2) { server.enqueue(MockResponse().setBody("""{"location":{"directory":"/p"},"data":[{"name":"review"}]}""")) }

        catalog.commands("/p")
        catalog.commands("/p")
        assertEquals(1, server.requestCount)

        now += 6 * 60_000L
        assertEquals(listOf("review"), catalog.commands("/p").getOrThrow().map { it.name })
        assertEquals(2, server.requestCount)
    }

    private fun assistant(id: String, created: Int, completed: Int?) =
        """{"id":"$id","time":{"created":$created${completed?.let { ",\"completed\":$it" }.orEmpty()}},"type":"assistant",""" +
            """"agent":"build","model":{"id":"m","providerID":"p"},"content":[]}"""

    private fun page(vararg newestFirst: String, next: String? = null) =
        MockResponse().setBody("""{"data":[${newestFirst.joinToString(",")}],"cursor":{${next?.let { "\"next\":\"$it\"" }.orEmpty()}}}""")

    @Test
    fun `a synced transcript is cached and later syncs fetch only newer messages`() = runBlocking {
        val cache = InMemoryMessageCache()
        val chat = RemoteChatRepository(graph.clients, cache)
        server.enqueue(page(userMessage(2), userMessage(1)))
        chat.messages("ses_1").getOrThrow()
        server.takeRequest()

        assertEquals(listOf("msg_1", "msg_2"), chat.cachedMessages("ses_1").map { it.id })

        // The newest page holds two new messages, then the last cached one: paging stops there.
        server.enqueue(page(userMessage(4), userMessage(3), userMessage(2), userMessage(1), next = "c1"))
        val synced = chat.messages("ses_1").getOrThrow()

        assertEquals(listOf("msg_1", "msg_2", "msg_3", "msg_4"), synced.map { it.id })
        assertEquals("/api/session/ses_1/message?limit=50", server.takeRequest().path)
        assertEquals(2, server.requestCount)
        assertEquals(synced, chat.cachedMessages("ses_1"))
    }

    @Test
    fun `a running step is fetched again until it completes`() = runBlocking {
        val cache = InMemoryMessageCache()
        val chat = RemoteChatRepository(graph.clients, cache)
        server.enqueue(page(assistant("msg_2", 2, completed = null), userMessage(1)))
        chat.messages("ses_1").getOrThrow()

        server.enqueue(page(assistant("msg_2", 2, completed = 9), userMessage(1)))
        val synced = chat.messages("ses_1").getOrThrow()

        assertEquals(9L, (synced.last() as Message.Assistant).completedAt)
        assertEquals(9L, (chat.cachedMessages("ses_1").last() as Message.Assistant).completedAt)
    }

    @Test
    fun `history is fetched again when the cached anchor is gone or unreadable`() = runBlocking {
        val cache = InMemoryMessageCache()
        val chat = RemoteChatRepository(graph.clients, cache)
        server.enqueue(page(userMessage(2), userMessage(1)))
        chat.messages("ses_1").getOrThrow()

        // The server no longer returns msg_2 (e.g. reverted): the cache is replaced.
        server.enqueue(page(userMessage(5), userMessage(1)))
        assertEquals(listOf("msg_1", "msg_5"), chat.messages("ses_1").getOrThrow().map { it.id })

        cache.sessions["s1" to "ses_1"] = CachedTranscript(listOf(CachedEntry("msg_1", "{not json")))
        assertTrue(chat.cachedMessages("ses_1").isEmpty())
        server.enqueue(page(userMessage(6)))
        assertEquals(listOf("msg_6"), chat.messages("ses_1").getOrThrow().map { it.id })
    }

    @Test
    fun `removing a server forgets its cached transcripts`() = runBlocking {
        val cache = InMemoryMessageCache()
        cache.sessions["s1" to "ses_1"] = CachedTranscript(listOf(CachedEntry("msg_1", userMessage(1))))
        val servers = StoredServerRepository(
            FakeServerStore(StoredServers(listOf(profile("s1", server.baseUrl())), activeId = "s1")),
            graph.factory,
            onRemoved = cache::clearServer,
        )

        servers.remove("s1")

        assertTrue(cache.sessions.isEmpty())
    }

    @Test
    fun `prompt, switches and permission replies send the documented bodies`() = runBlocking {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(204)) }
        val chat = RemoteChatRepository(graph.clients, InMemoryMessageCache())

        chat.prompt("ses_1", "hi", PromptDelivery.Queue).getOrThrow()
        chat.switchAgent("ses_1", "plan").getOrThrow()
        chat.switchModel("ses_1", ModelRef("m", "p", "high")).getOrThrow()
        chat.replyPermission("ses_1", "per_1", PermissionDecision.Always).getOrThrow()

        val prompt = server.takeRequest()
        assertEquals("/api/session/ses_1/prompt", prompt.path)
        assertEquals("""{"text":"hi","delivery":"queue"}""", prompt.body.readUtf8())
        assertEquals("""{"agent":"plan"}""", server.takeRequest().body.readUtf8())
        assertEquals("""{"model":{"id":"m","providerID":"p","variant":"high"}}""", server.takeRequest().body.readUtf8())
        val reply = server.takeRequest()
        assertEquals("/api/session/ses_1/permission/per_1/reply", reply.path)
        assertEquals("""{"decision":"always"}""", reply.body.readUtf8())
    }

    @Test
    fun `sessions list asks for root sessions newest first`() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        server.enqueue(MockResponse().setBody("""{"data":{"ses_a":{"type":"running"}}}"""))

        val repository = RemoteSessionRepository(graph.clients)
        val sessions = repository.list(search = " probe ").getOrThrow()

        assertEquals("event probe", sessions.single().title)
        assertEquals("/api/session?limit=100&order=desc&search=probe&parentID=null", server.takeRequest().path)
        assertEquals(setOf("ses_a"), repository.active().getOrThrow())
    }

    @Test
    fun `rename and delete keep the session cache in sync`() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("session_get.json")))
        val repository = RemoteSessionRepository(graph.clients)
        val session = repository.get("ses_ec9cb5c70ffe3M22jWcPWdSAIF").getOrThrow()
        assertEquals(session.title, repository.cached(session.id)?.title)

        server.enqueue(MockResponse().setResponseCode(200))
        repository.rename(session.id, "Renamed").getOrThrow()
        assertEquals("Renamed", repository.cached(session.id)?.title)

        server.enqueue(MockResponse().setResponseCode(204))
        repository.delete(session.id).getOrThrow()
        assertNull(repository.cached(session.id))
    }

    @Test
    fun `create sends location, agent and model`() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("session_get.json")))

        val session = RemoteSessionRepository(graph.clients)
            .create("/tmp/x", agent = "build", model = ModelRef("claude-haiku-5-5", "anthropic"))
            .getOrThrow()

        assertEquals("ses_ec9cb5c70ffe3M22jWcPWdSAIF", session.id)
        assertEquals(
            """{"agent":"build","model":{"id":"claude-haiku-5-5","providerID":"anthropic"},"location":{"directory":"/tmp/x"}}""",
            server.takeRequest().body.readUtf8(),
        )
    }

    /** Answers catalog requests by path because models and providers load concurrently. */
    private fun catalogDispatcher(providers: MockResponse) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
            "/api/model" -> MockResponse().setBody(fixture("model_list.json"))
            "/api/provider" -> providers
            "/api/agent" -> MockResponse().setBody(fixture("agent_list.json"))
            else -> MockResponse().setResponseCode(404)
        }
    }

    @Test
    fun `catalog keeps enabled models newest first and scopes by directory`() = runBlocking {
        server.dispatcher = catalogDispatcher(
            MockResponse().setBody(
                """{"location":{"directory":"/tmp/x"},"data":[{"id":"anthropic","integrationID":"anthropic","name":"Anthropic",""" +
                    """"activation":"enabled","package":"@opencode/ai/providers/anthropic","settings":{"apiKey":"not-a-real-key"}}]}""",
            ),
        )
        val catalog = RemoteCatalogRepository(graph.clients)

        val models = catalog.models("/tmp/x").getOrThrow()
        val agents = catalog.agents(null).getOrThrow()

        assertEquals("claude-haiku-5-5", models.first().ref.id)
        assertEquals("Anthropic", models.first().providerName)
        assertEquals(models.sortedByDescending { it.releasedAt }, models)
        val paths = List(server.requestCount) { server.takeRequest().path }
        assertTrue("/api/model?location%5Bdirectory%5D=%2Ftmp%2Fx" in paths)
        assertTrue("/api/provider?location%5Bdirectory%5D=%2Ftmp%2Fx" in paths)
        assertEquals("build", agents.first().id)
    }

    @Test
    fun `models keep provider ids when provider names cannot be loaded`() = runBlocking {
        server.dispatcher = catalogDispatcher(MockResponse().setResponseCode(500))

        val models = RemoteCatalogRepository(graph.clients).models(null).getOrThrow()

        assertEquals("anthropic", models.first().providerName)
    }

    @Test
    fun `no active server fails with Unauthorized`() = runBlocking {
        val empty = TestGraph(FakeServerStore(StoredServers()))

        assertTrue(RemoteSessionRepository(empty.clients).list().exceptionOrNull() is OpenCodeError.Unauthorized)
    }
}
