package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.cache.InMemoryMessageCache
import io.github.beppe2880.opencodemobile.core.data.chat.RemoteChatRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SideQuestionRepositoryTest {
    private val server = MockWebServer()
    private val cache = InMemoryMessageCache()
    private lateinit var chat: RemoteChatRepository

    @Before
    fun setUp() {
        server.start()
        chat = RemoteChatRepository(TestGraph.activeOn(server).clients, cache)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun generated(text: String) =
        """{"data":{"text":"$text","tokens":{"input":1,"output":1,"reasoning":0,"cache":{"read":0,"write":0}},"cost":0}}"""

    @Test
    fun `generation uses the session endpoint and leaves the transcript cache alone`() = runBlocking {
        server.enqueue(MockResponse().setBody(generated("An answer")))
        assertEquals("An answer", chat.generate("ses_test", "A question").getOrThrow())
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/session/ses_test/generate", request.path)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("prompt"), body.keys)
        assertEquals("A question", body.getValue("prompt").jsonPrimitive.content)
        assertTrue(cache.sessions.isEmpty())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `servers before 2_3 answer without token usage`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"text":"Old server answer"}}"""))
        assertEquals("Old server answer", chat.generate("ses_test", "A question").getOrThrow())
    }

    @Test
    fun `a side question stays pinned to its original server after navigation changes the active server`() = runBlocking {
        val other = MockWebServer()
        other.start()
        try {
            val graph = TestGraph(
                FakeServerStore(
                    StoredServers(listOf(profile("s1", server.baseUrl()), profile("s2", other.baseUrl())), activeId = "s2"),
                ),
            )
            val repository = RemoteChatRepository(graph.clients, cache)
            server.enqueue(MockResponse().setBody(generated("Original server")))
            assertEquals("Original server", repository.generate("ses_test", "Question", serverId = "s1").getOrThrow())
            assertEquals(1, server.requestCount)
            assertEquals(0, other.requestCount)
        } finally {
            other.shutdown()
        }
    }

    @Test
    fun `a removed server fails without sending the question to the active server`() = runBlocking {
        assertTrue(chat.generate("ses_test", "Question", serverId = "removed").exceptionOrNull() is OpenCodeError.NotFound)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `unsupported endpoint and authentication failures stay typed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"_tag":"NotFoundError","message":"Not found"}"""))
        assertTrue(chat.generate("ses_test", "Question").exceptionOrNull() is OpenCodeError.NotFound)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Unauthorized"}"""))
        assertTrue(chat.generate("ses_test", "Question").exceptionOrNull() is OpenCodeError.Unauthorized)
    }
}
