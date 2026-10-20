package io.github.beppe2880.opencodemobile.core.data.server

import io.github.beppe2880.opencodemobile.core.data.FakeServerStore
import io.github.beppe2880.opencodemobile.core.data.TestGraph
import io.github.beppe2880.opencodemobile.core.data.baseUrl
import io.github.beppe2880.opencodemobile.core.data.profile
import io.github.beppe2880.opencodemobile.core.domain.ConnectTarget
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClientFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StoredServerRepositoryTest {

    private val server = MockWebServer()
    private val store = FakeServerStore()
    private val info = """{"version":"2.0.27","pid":1,"urls":[],"paths":{"tmp":"/tmp"}}"""
    private lateinit var url: String

    @Before
    fun setUp() {
        server.start()
        url = server.baseUrl()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `password is validated before the server is saved and activated`() = runBlocking {
        server.enqueue(MockResponse().setBody(info))
        val repository = TestGraph(store).servers

        val saved = repository.addWithPassword("$url/", "s3cret", name = " Desk ").getOrThrow()

        assertEquals("Desk", saved.name)
        assertEquals(url, saved.baseUrl)
        assertEquals(CredentialKind.Password, saved.credential)
        assertEquals(saved, repository.activeServer.value)
        assertEquals(listOf(saved), repository.servers.value)
        assertEquals(StoredServers(listOf(saved), saved.id), store.stored)
        assertTrue(server.takeRequest().getHeader("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun `wrong password saves nothing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Authentication required"}"""))
        val repository = TestGraph(store).servers

        val error = repository.addWithPassword(url, "nope").exceptionOrNull()

        assertTrue(error is OpenCodeError.Unauthorized)
        assertNull(repository.activeServer.value)
        assertEquals(0, store.saves)
    }

    @Test
    fun `pairing redeems the code then stores the token`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"token":"tok_1"}"""))
        server.enqueue(MockResponse().setBody(info))
        val repository = TestGraph(store).servers

        val saved = repository.pair(url, "CODE").getOrThrow()

        assertEquals("tok_1", saved.secret)
        assertEquals(CredentialKind.PairingToken, saved.credential)
        assertEquals("/auth/connect/CODE", server.takeRequest().path)
        assertEquals("/api/info", server.takeRequest().path)
        assertEquals(saved, repository.activeServer.value)
    }

    @Test
    fun `expired pairing code is reported as PairingFailed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Pairing link expired or already used"}"""))

        val error = TestGraph(store).servers.pair(url, "OLD").exceptionOrNull()

        assertTrue(error is OpenCodeError.PairingFailed)
        assertEquals(0, store.saves)
    }

    @Test
    fun `probe without password reports Unauthorized`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        assertTrue(TestGraph(store).servers.probe(url, null).exceptionOrNull() is OpenCodeError.Unauthorized)
        assertEquals(null, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `invalid address fails without a request`() = runBlocking {
        assertTrue(TestGraph(store).servers.probe("not a url", "pw").exceptionOrNull() is OpenCodeError.Unreachable)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `saving a known address updates the existing entry`() = runBlocking {
        store.stored = StoredServers(listOf(profile("old", url, secret = "old-pw")), activeId = null)
        server.enqueue(MockResponse().setBody(info))
        val repository = TestGraph(store).servers

        val saved = repository.addWithPassword(url, "new-pw").getOrThrow()

        assertEquals("old", saved.id)
        assertEquals("new-pw", saved.secret)
        assertEquals(listOf(saved), repository.servers.value)
    }

    @Test
    fun `restores the active server and falls back when it is removed`() = runBlocking {
        val older = profile("a", "http://a:4096", addedAt = 1)
        val newer = profile("b", "http://b:4096", addedAt = 2)
        store.stored = StoredServers(listOf(older, newer), activeId = "a")
        val repository = TestGraph(store).servers

        assertEquals(older, repository.activeServer.value)
        assertEquals(listOf(newer, older), repository.servers.value)

        repository.remove("a")
        assertEquals(newer, repository.activeServer.value)

        repository.rename("b", "Laptop")
        assertEquals("Laptop", repository.activeServer.value?.name)
        assertEquals("Laptop", store.stored.profiles.single().name)
    }

    @Test
    fun `servers are read in the background and changes wait for them`() = runTest {
        store.stored = StoredServers(listOf(profile("a", "http://a:4096")), activeId = "a")
        val io = StandardTestDispatcher(testScheduler)
        val repository = StoredServerRepository(store, OpenCodeClientFactory(), io, scope = backgroundScope)

        assertFalse(repository.loaded.value)
        assertNull(repository.activeServer.value)

        repository.rename("a", "Desk")

        assertTrue(repository.loaded.value)
        assertEquals("Desk", repository.activeServer.value?.name)
        assertEquals("Desk", store.stored.profiles.single().name)
    }

    @Test
    fun `stale active id is ignored`() {
        store.stored = StoredServers(listOf(profile("a", "http://a:4096")), activeId = "gone")
        assertNull(TestGraph(store).servers.activeServer.value)
    }

    @Test
    fun `parses connect targets`() {
        assertEquals(
            ConnectTarget.Pairing("http://10.0.2.2:4096", "abc"),
            TestGraph(store).servers.parseConnectTarget("http://10.0.2.2:4096/auth/connect/abc"),
        )
    }

    @Test
    fun `a store that fails to load leaves the repository empty but usable`() = runTest {
        val failing = object : ServerStore {
            override fun load(): StoredServers = throw SecurityException("keystore unavailable")
            override fun save(servers: StoredServers) = Unit
        }
        val repository = StoredServerRepository(failing, OpenCodeClientFactory())

        repository.loaded.first { it }
        assertEquals(emptyList<ServerProfile>(), repository.servers.value)
        assertNull(repository.activeServer.value)
    }
}

