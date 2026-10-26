package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.cache.InMemoryListCache
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import kotlinx.coroutines.runBlocking
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
    fun `unsearched lists and projects are stored per server and directory`() = runBlocking {
        val repository = RemoteSessionRepository(graph.clients, lists)
        assertNull(repository.cachedList("s1"))

        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        val loaded = repository.list(directory = "/tmp/x").getOrThrow()
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
        repository.list(search = "probe").getOrThrow()
        server.enqueue(MockResponse().setBody(fixture("project_list.json")))
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
        server.enqueue(MockResponse().setBody(fixture("session_list.json")))
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
        val prefs = InMemoryPreferences()
        val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))
        PreferencesDraftRepository(prefs, TestGraph(store).clients).save("ses_1", "mine")
        PreferencesDraftRepository(prefs, TestGraph(FakeServerStore(store.stored.copy(activeId = "s2"))).clients).save("ses_1", "other")

        PreferencesDraftRepository.clearServer(prefs, "s1")

        assertEquals(setOf("s2/ses_1"), prefs.values.keys)
        PreferencesDraftRepository.clearServer(prefs, "unknown")
        assertEquals(setOf("s2/ses_1"), prefs.values.keys)
    }
}
