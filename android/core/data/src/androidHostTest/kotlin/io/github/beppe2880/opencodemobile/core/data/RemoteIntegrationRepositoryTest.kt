package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.integration.RemoteIntegrationRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteIntegrationRepositoryTest {
    private val server = MockWebServer()
    private lateinit var graph: TestGraph
    private lateinit var repository: RemoteIntegrationRepository

    @Before fun setup() {
        server.start()
        graph = TestGraph.activeOn(server)
        repository = RemoteIntegrationRepository(graph.clients, graph.servers)
    }

    @After fun close() = server.shutdown()

    @Test fun `discover all key integrations and preserve additional fields`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"location":{"directory":"/repo"},"data":[
            {"id":"custom-z","name":"Z custom","methods":[{"type":"key","form":[{"type":"string","key":"endpoint","required":true,"default":"https://example.test","minLength":3}]}],"connections":[]},
            {"id":"oauth-only","name":"OAuth","methods":[{"type":"oauth","id":"browser","label":"Browser"}],"connections":[]},
            {"id":"mcp","name":"MCP","metadata":{"source":"mcp"},"methods":[{"type":"key"}],"connections":[]},
            {"id":"another","name":"A provider","methods":[{"type":"oauth","id":"device","label":"Device"},{"type":"key","label":"Token"}],"connections":[]}
        ]}""",
            ),
        )
        val list = repository.listKeys("s1", "/repo").getOrThrow()
        assertEquals(listOf("another", "custom-z"), list.map { it.id })
        assertEquals("Token", list.first().keyLabel)
        assertFalse(list.last().isComplete(emptyMap()))
        assertTrue(list.last().isComplete(list.last().defaults()))
        assertEquals("/repo", server.takeRequest().requestUrl!!.queryParameter("location[directory]"))
    }

    @Test fun `key and answers use the original saved server even after switching`() = runBlocking {
        graph = TestGraph(FakeServerStore(StoredServers(listOf(profile("s1", server.baseUrl()), profile("s2", "http://127.0.0.1:1")), activeId = "s2")))
        repository = RemoteIntegrationRepository(graph.clients, graph.servers)
        // The original saved profile stays addressable without consulting active().
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repository.connectKey("s1", "/repo", "custom", "dummy-test-key", mapOf("region" to FormAnswer.Text("eu"))).isSuccess)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/integration/custom/connect/key", request.requestUrl!!.encodedPath)
        assertEquals("/repo", request.requestUrl!!.queryParameter("location[directory]"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("dummy-test-key", body["key"]!!.jsonPrimitive.content)
        assertEquals("eu", body["answer"]!!.jsonObject["region"]!!.jsonPrimitive.content)
    }

    @Test fun `failures are typed and removed profiles never fall back to active server`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Denied"}"""))
        assertTrue(repository.listKeys("s1", "/repo").exceptionOrNull() is OpenCodeError.Unauthorized)
        assertTrue(repository.connectKey("removed", "/repo", "custom", "dummy", emptyMap()).exceptionOrNull() is OpenCodeError.NotFound)
    }
}
