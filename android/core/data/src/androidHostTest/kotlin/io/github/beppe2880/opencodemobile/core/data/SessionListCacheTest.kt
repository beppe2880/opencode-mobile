package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.cache.InMemoryListCache
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.SessionExportProgress
import io.github.beppe2880.opencodemobile.core.domain.SessionExportStage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionListCacheTest {

    private val server = MockWebServer()
    private val lists = InMemoryListCache()
    private lateinit var graph: TestGraph

    @Before
    fun setUp() {
        server.start()
        graph = TestGraph.activeOn(server)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String) = requireNotNull(javaClass.classLoader?.getResource(name)).readText()

    @Test
    fun `export requests full original text from the generated API and maps all messages`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        val info = Json.parseToJsonElement(fixture("session_get.json")).jsonObject.getValue("data")
        val messages = Json.parseToJsonElement(fixture("session_messages.json")).jsonObject.getValue("data")
        val body = JsonObject(mapOf("data" to JsonObject(mapOf("info" to info, "messages" to messages))))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body.toString()))
        val exported = repository.export("ses_test").getOrThrow()
        val request = server.takeRequest()
        assertEquals("/api/experimental/session/ses_test/export?sanitize=false", request.path)
        assertTrue(exported.any { it is Message.User })
        assertTrue(exported.any { it is Message.Assistant })
        server.enqueue(MockResponse().setResponseCode(404))
        assertTrue(repository.export("ses_test").exceptionOrNull() is OpenCodeError.NotFound)
    }

    @Test
    fun `export accepts legacy provider metadata and current native metadata with identical content`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        val info = Json.parseToJsonElement(fixture("session_get.json")).jsonObject.getValue("data")
        val original = Json.parseToJsonElement(fixture("session_messages.json")).jsonObject.getValue("data").jsonArray
        val metadata = JsonObject(mapOf("opaque" to JsonPrimitive("fixture-metadata")))
        val baseline = JsonObject(mapOf("data" to JsonObject(mapOf("info" to info, "messages" to original))))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(baseline.toString()))
        val expected = repository.export("ses_test").getOrThrow()
        server.takeRequest()

        // Contract refresh #246 replaced optional providerState/state with native, and added
        // optional time.firstToken. None should change the user-facing export or require 2.3.2.
        for (legacy in listOf(true, false)) {
            val messages = JsonArray(
                original.map { element ->
                    val message = element.jsonObject
                    if (message["type"] != JsonPrimitive("assistant")) return@map element
                    val content = JsonArray(
                        message.getValue("content").jsonArray.map { item ->
                            val part = item.jsonObject
                            val key = if (legacy) {
                                if (part["type"] == JsonPrimitive("tool")) "providerState" else "state"
                            } else {
                                "native"
                            }
                            val fields = part - "native" - "providerState" - "providerResultState"
                            // A tool's state is its actual input/output, not provider metadata.
                            val clean = if (part["type"] == JsonPrimitive("tool")) fields else fields - "state"
                            JsonObject(clean + (key to metadata))
                        },
                    )
                    val time = message.getValue("time").jsonObject
                    JsonObject(
                        (message - "providerState" - "native") + mapOf(
                            (if (legacy) "providerState" else "native") to metadata,
                            "content" to content,
                            "time" to if (legacy) time else JsonObject(time + ("firstToken" to time.getValue("created"))),
                        ),
                    )
                },
            )
            val body = JsonObject(mapOf("data" to JsonObject(mapOf("info" to info, "messages" to messages))))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body.toString()))
            assertEquals(expected, repository.export("ses_test").getOrThrow())
            assertEquals("/api/experimental/session/ses_test/export?sanitize=false", server.takeRequest().path)
        }
    }

    @Test
    fun `JSON export preserves native metadata and unknown messages without the HTTP envelope`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        val info = Json.parseToJsonElement(fixture("session_get.json")).jsonObject.getValue("data").jsonObject
        val original = Json.parseToJsonElement(fixture("session_messages.json")).jsonObject.getValue("data").jsonArray
        val unknown = Json.parseToJsonElement("""{"type":"future-message","native":{"providerState":"retained"}}""")
        val data = JsonObject(
            mapOf(
                "info" to JsonObject(info + ("future-info" to JsonPrimitive("retained"))),
                "messages" to JsonArray(original + unknown),
                "future-field" to JsonPrimitive("retained"),
            ),
        )
        server.enqueue(MockResponse().setBody(JsonObject(mapOf("data" to data)).toString()))
        val exported = Json.parseToJsonElement(repository.exportJson("ses_test").getOrThrow())
        assertEquals(data, exported)
        assertEquals("/api/experimental/session/ses_test/export?sanitize=false", server.takeRequest().path)
    }

    @Test
    fun `repeat exports download the current session again`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        for (title in listOf("First version", "Updated version")) {
            server.enqueue(MockResponse().setBody("""{"data":{"info":{"title":"$title"},"messages":[]}}"""))
            val stages = mutableListOf<SessionExportProgress>()
            val exported = Json.parseToJsonElement(repository.exportJson("ses_test", stages::add).getOrThrow()).jsonObject
            assertEquals(JsonPrimitive(title), exported.getValue("info").jsonObject.getValue("title"))
            assertEquals("/api/experimental/session/ses_test/export?sanitize=false", server.takeRequest().path)
            assertEquals(SessionExportStage.WaitingForServer, stages.first().stage)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `JSON export rejects invalid documents and preserves HTTP failures`() = runBlocking<Unit> {
        val repository = RemoteSessionRepository(graph.clients, lists)
        for (body in listOf("not json", "{}", """{"data":{"info":{},"messages":"invalid"}}""")) {
            server.enqueue(MockResponse().setBody(body))
            assertTrue(repository.exportJson("ses_test").exceptionOrNull() is OpenCodeError.Server)
            server.takeRequest()
        }
        server.enqueue(MockResponse().setResponseCode(404))
        assertTrue(repository.exportJson("ses_test").exceptionOrNull() is OpenCodeError.NotFound)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(401))
        assertTrue(repository.exportJson("ses_test").exceptionOrNull() is OpenCodeError.Unauthorized)
        server.takeRequest()
    }

    @Test
    fun `unsearched lists and projects are stored per server and directory`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        assertNull(repository.cachedList("s1"))

        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture("session_list.json")))
        val loaded = repository.list(directory = "/tmp/x").getOrThrow()
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture("session_list.json")))
        repository.list(search = "probe").getOrThrow()
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture("project_list.json")))
        val projects = repository.projects().getOrThrow()

        assertEquals(loaded, repository.cachedList("s1", "/tmp/x"))
        // Searches are not stored: they would replace the full list shown on the next start.
        assertNull(repository.cachedList("s1"))
        assertNull(repository.cachedList("s2", "/tmp/x"))
        assertEquals(projects, repository.cachedProjects("s1"))

        lists.clearServer("s1")
        assertNull(repository.cachedList("s1", "/tmp/x"))
        assertNull(repository.cachedProjects("s1"))
    }

    @Test
    fun `a failed load keeps the stored list`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture("session_list.json")))
        val loaded = repository.list().getOrThrow()
        server.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repository.list().isFailure)
        assertEquals(loaded, repository.cachedList("s1"))
    }

    @Test
    fun `deleting a session removes it from every saved list of that server`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        val loaded = repository.list().getOrThrow()
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        repository.list(directory = "/tmp/other").getOrThrow()
        lists.write("s2", ListCache.sessionsKey(null), lists.lists.getValue("s1" to ListCache.sessionsKey(null)))
        val deletedId = loaded.single().id
        server.enqueue(MockResponse().setResponseCode(204))

        repository.delete(deletedId).getOrThrow()

        assertTrue(repository.cachedList("s1")?.isEmpty() == true)
        assertTrue(repository.cachedList("s1", "/tmp/other")?.isEmpty() == true)
        assertEquals(loaded, repository.cachedList("s2"))
        assertNull(repository.cached(deletedId))
    }

    @Test
    fun `a failed delete keeps the saved lists`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        val loaded = repository.list().getOrThrow()
        server.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repository.delete(loaded.single().id).isFailure)
        assertEquals(loaded, repository.cachedList("s1"))
    }

    @Test
    fun `clearing a server deletes only its drafts`() {
        val values = InMemoryPreferences()
        val prefs = SharedPreferencesSettings(values)
        val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))
        PreferencesDraftRepository(prefs, TestGraph(store).clients).save("ses_1", "mine")
        PreferencesDraftRepository(prefs, TestGraph(FakeServerStore(store.stored.copy(activeId = "s2"))).clients).save("ses_1", "other")

        PreferencesDraftRepository.clearServer(prefs, "s1")

        assertEquals(setOf("s2/ses_1"), values.values.keys)
        PreferencesDraftRepository.clearServer(prefs, "unknown")
        assertEquals(setOf("s2/ses_1"), values.values.keys)
    }

    @Test
    fun `archived lists are not cached and request the archived filter`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))

        repository.list(archived = true).getOrThrow()

        assertTrue(server.takeRequest().path!!.contains("archived=true"))
        assertNull(repository.cachedList("s1"))
    }

    @Test
    fun `archiving patches the session and drops it from the stored list`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        val loaded = repository.list().getOrThrow()
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(204))

        repository.setArchived(loaded.single().id, true).getOrThrow()

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertTrue(request.path!!.endsWith("/api/session/${loaded.single().id}"))
        assertTrue(request.body.readUtf8().contains("\"archived\":true"))
        assertTrue(repository.cachedList("s1")?.none { it.id == loaded.single().id } != false)
    }
}
