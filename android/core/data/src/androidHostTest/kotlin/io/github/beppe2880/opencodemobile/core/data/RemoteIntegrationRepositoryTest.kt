package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.integration.BrowserOAuth
import io.github.beppe2880.opencodemobile.core.data.integration.RemoteIntegrationRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStatus
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
    private val browserDiscovery = """{"location":{"directory":"/repo"},"data":[
        {"id":"openai","name":"OpenAI","methods":[{"type":"oauth","id":"chatgpt-token-sharing","label":"Sign in with ChatGPT"}],"connections":[]}
    ]}"""

    private fun enqueueBrowserPreparation() {
        server.enqueue(MockResponse().setBody(browserDiscovery))
        server.enqueue(
            MockResponse().setBody(
                """{"location":{"directory":"/repo"},"data":{
            "attemptID":"prepare","url":"https://auth.openai.com/api/accounts/authorize?ext_agent_host_id=urn:uuid:test-server",
            "instructions":"","mode":"auto","time":{"created":1,"expires":9999999999999}}}""",
            ),
        )
        server.enqueue(MockResponse().setResponseCode(204))
    }

    @Test fun `validated browser credential imports client and scopes without overwriting server host identity`() = runBlocking {
        val fixture = OAuthFixture()
        repository = RemoteIntegrationRepository(graph.clients, graph.servers, BrowserOAuth(fixture, fixture.settings))
        enqueueBrowserPreparation()
        server.enqueue(MockResponse().setBody("{}"))
        repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", fixture::opened).getOrThrow()
        assertEquals("/api/integration", server.takeRequest().requestUrl!!.encodedPath)
        assertEquals("POST", server.takeRequest().method)
        assertEquals("DELETE", server.takeRequest().method)
        val request = server.takeRequest()
        assertEquals("/api/credential", request.requestUrl!!.encodedPath)
        assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("openai", body["integrationID"]!!.jsonPrimitive.content)
        assertEquals("true", body["activate"]!!.jsonPrimitive.content)
        val value = body["value"]!!.jsonObject
        assertEquals("chatgpt-token-sharing", value["methodID"]!!.jsonPrimitive.content)
        assertEquals("test-refresh", value["refresh"]!!.jsonPrimitive.content)
        assertEquals("issued-client", value["metadata"]!!.jsonObject["clientID"]!!.jsonPrimitive.content)
        assertFalse(value.toString().contains("ext_agent_host_id"))
        assertFalse(value.toString().contains("id_token"))
    }

    @Test fun `credential import does not follow redirects`() = runBlocking {
        val fixture = OAuthFixture()
        repository = RemoteIntegrationRepository(graph.clients, graph.servers, BrowserOAuth(fixture, fixture.settings))
        enqueueBrowserPreparation()
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", server.url("/leak")))
        val failure = repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", fixture::opened).exceptionOrNull() as ProviderOAuthError
        assertEquals("OAUTH_Import_HTTP_307", failure.diagnosticCode)
        assertEquals(4, server.requestCount)
        assertTrue(fixture.closed)
    }

    @Test fun `import failure preserves only phase and status and preparation failures remain separate`() = runBlocking {
        val fixture = OAuthFixture()
        repository = RemoteIntegrationRepository(graph.clients, graph.servers, BrowserOAuth(fixture, fixture.settings))
        enqueueBrowserPreparation()
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"test-refresh secret-code"}"""))
        val failure = repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", fixture::opened).exceptionOrNull() as ProviderOAuthError
        assertEquals("OAUTH_Import_HTTP_403", failure.diagnosticCode)
        assertEquals(null, failure.cause)
        assertFalse(failure.toString().contains("test-refresh"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"secret-code"}"""))
        val preparation = repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", fixture::opened)
            .exceptionOrNull() as ProviderOAuthError
        assertEquals("OAUTH_Preparation_HTTP_401", preparation.diagnosticCode)
    }

    @Test fun `server switch and invalid tokens never import credentials`() = runBlocking {
        graph = TestGraph(
            FakeServerStore(
                StoredServers(
                    listOf(profile("s1", server.baseUrl()), profile("s2", "http://127.0.0.1:1")), activeId = "s1",
                ),
            ),
        )
        val fixture = OAuthFixture().apply { badNonce = true }
        repository = RemoteIntegrationRepository(graph.clients, graph.servers, BrowserOAuth(fixture, fixture.settings))
        enqueueBrowserPreparation()
        assertTrue(repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", fixture::opened).isFailure)
        assertEquals(3, server.requestCount)
        fixture.badNonce = false
        enqueueBrowserPreparation()
        assertTrue(
            repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing") {
                fixture.opened(it)
                graph.servers.activate("s2")
            }.isFailure,
        )
        assertEquals(6, server.requestCount)
    }

    @Test fun `plain remote transport and unknown methods are refused before browser launch`() = runBlocking {
        val fixture = OAuthFixture()
        graph = TestGraph(FakeServerStore(StoredServers(listOf(profile("s1", "http://192.168.1.2:4096")), activeId = "s1")))
        repository = RemoteIntegrationRepository(graph.clients, graph.servers, BrowserOAuth(fixture, fixture.settings))
        assertTrue(repository.connectBrowserOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", fixture::opened).isFailure)
        assertTrue(repository.connectBrowserOAuth("s1", "/repo", "unknown", "browser", fixture::opened).isFailure)
        assertEquals(null, fixture.authorization)
        assertEquals(0, server.requestCount)
    }

    @Test fun `device attempt and status preserve original server and location`() = runBlocking {
        val time = """"time":{"created":1,"expires":9999999999999}"""
        server.enqueue(
            MockResponse().setBody(
                """{"location":{"directory":"/repo"},"data":{
            "attemptID":"attempt","url":"https://example.test/device",
            "instructions":"Authorize in browser","mode":"auto",$time}}""",
            ),
        )
        val attempt = repository.startOAuth("s1", "/repo", "github-copilot", "device", emptyMap()).getOrThrow()
        assertEquals("attempt", attempt.id)
        val start = server.takeRequest()
        assertEquals("/api/integration/github-copilot/connect/oauth", start.requestUrl!!.encodedPath)
        assertEquals("/repo", start.requestUrl!!.queryParameter("location[directory]"))
        assertEquals("device", Json.parseToJsonElement(start.body.readUtf8()).jsonObject["methodID"]!!.jsonPrimitive.content)
        val statuses = listOf(
            "pending" to ProviderOAuthStatus.Pending,
            "complete" to ProviderOAuthStatus.Complete,
            "expired" to ProviderOAuthStatus.Expired,
        )
        statuses.forEach { (status, expected) ->
            server.enqueue(MockResponse().setBody("""{"location":{"directory":"/repo"},"data":{"status":"$status",$time}}"""))
            assertEquals(expected, repository.oauthStatus("s1", "/repo", "github-copilot", "attempt").getOrThrow())
            assertEquals("/repo", server.takeRequest().requestUrl!!.queryParameter("location[directory]"))
        }
        server.enqueue(MockResponse().setResponseCode(204))
        repository.cancelOAuth("s1", "/repo", "github-copilot", "attempt").getOrThrow()
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test fun `unsafe authorization URL is cancelled and browser flows cannot start`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"location":{"directory":"/repo"},"data":{
            "attemptID":"attempt","url":"http://127.0.0.1:1234/callback","instructions":"Never open",
            "mode":"auto","time":{"created":1,"expires":9999999999999}}}""",
            ),
        )
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repository.startOAuth("s1", "/repo", "xai", "device", emptyMap()).isFailure)
        server.takeRequest()
        assertEquals("DELETE", server.takeRequest().method)
        assertTrue(repository.startOAuth("s1", "/repo", "openai", "chatgpt-token-sharing", emptyMap()).isFailure)
        assertEquals(2, server.requestCount)
    }
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
        assertEquals(listOf("another", "oauth-only", "custom-z"), list.map { it.id })
        assertFalse(list[1].hasKey)
        assertFalse(list[1].oauthMethods.single().mobileSupported)
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
