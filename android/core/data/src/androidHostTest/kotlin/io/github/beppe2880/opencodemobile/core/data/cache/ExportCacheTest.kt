package io.github.beppe2880.opencodemobile.core.data.cache

import androidx.room.Room
import io.github.beppe2880.opencodemobile.core.data.TestGraph
import io.github.beppe2880.opencodemobile.core.data.session.RemoteSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SessionExportProgress
import io.github.beppe2880.opencodemobile.core.domain.SessionExportStage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExportCacheTest {
    private lateinit var db: CacheDatabase
    private lateinit var cache: ExportCache
    private var now = 0L
    private var calls = 0

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), CacheDatabase::class.java).build()
        cache = ExportCache(db.exports(), clock = { ++now })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun load(server: String = "s", session: String = "chat"): String =
        cache.load(server, session) { NativeExport("response-${++calls}") }.json

    @Test
    fun `repository shares the complete JSON across formats without another HTTP request and rename invalidates`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val graph = TestGraph.activeOn(server)
            val repository = RemoteSessionRepository(graph.clients, exports = cache)
            fun fixture(name: String) = Json.parseToJsonElement(requireNotNull(javaClass.classLoader?.getResource(name)).readText()).jsonObject
            val native =
                JsonObject(mapOf("info" to fixture("session_get.json").getValue("data"), "messages" to fixture("session_messages.json").getValue("data")))
            val body = JsonObject(mapOf("data" to native)).toString()
            cache.connection("s1", true)
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))
            val stages = mutableListOf<SessionExportProgress>()
            assertEquals(native.toString(), repository.exportJson("ses_test", stages::add).getOrThrow())
            assertEquals(
                listOf(SessionExportStage.WaitingForServer, SessionExportStage.Downloading, SessionExportStage.Decoding, SessionExportStage.Preparing),
                stages.map { it.stage }.distinct(),
            )
            assertEquals(1f, stages.last { it.stage == SessionExportStage.Downloading }.fraction)
            stages.clear()
            assertTrue(repository.export("ses_test", stages::add).getOrThrow().isNotEmpty())
            assertEquals(listOf(SessionExportStage.Decoding, SessionExportStage.PreparingMessages), stages.map { it.stage }.distinct())
            assertEquals(1f, stages.last().fraction)
            stages.clear()
            assertEquals(native.toString(), repository.exportJson("ses_test", stages::add).getOrThrow())
            assertEquals(listOf(SessionExportStage.Preparing), stages.map { it.stage })
            assertEquals(1, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(204))
            repository.rename("ses_test", "renamed").getOrThrow()
            assertTrue(db.exports().entries().isEmpty())
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))
            repository.exportJson("ses_test").getOrThrow()
            assertEquals(3, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(204))
            repository.delete("ses_test").getOrThrow()
            assertTrue(db.exports().entries().isEmpty())
            assertTrue(db.exports().chunks("s1", "ses_test").isEmpty())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `hit and invalidation are scoped by server and session`() = runBlocking {
        cache.connection("s", true)
        cache.connection("other", true)
        assertEquals("response-1", load())
        assertEquals("response-1", load())
        assertEquals("response-2", load("other"))
        cache.invalidate("s", "chat")
        assertEquals("response-3", load())
        assertEquals("response-2", load("other"))
        cache.clearServer("s")
        assertTrue(db.exports().entries().none { it.serverId == "s" })
        assertEquals("response-2", load("other"))
    }

    @Test
    fun `disconnected restart and reconnect never trust a previous export`() = runBlocking {
        load()
        load()
        assertEquals(2, calls)
        cache.connection("s", true)
        load()
        load()
        assertEquals(3, calls)
        cache.connection("s", false)
        cache.connection("s", true)
        load()
        assertEquals(4, calls)
        cache = ExportCache(db.exports())
        load()
        assertEquals(5, calls)
    }

    @Test
    fun `concurrent loads share the fetch and invalidation during fetch prevents storage`() = runBlocking {
        cache.connection("s", true)
        coroutineScope {
            val started = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val first = async {
                cache.load("s", "chat") {
                    calls++
                    started.complete(Unit)
                    finish.await()
                    NativeExport("first")
                }
            }
            started.await()
            val second = async { load() }
            finish.complete(Unit)
            assertEquals("first", first.await().json)
            assertEquals("first", second.await())
            assertEquals(1, calls)
        }
        cache.invalidate("s", "chat")
        cache.load("s", "chat") {
            cache.invalidate("s", "chat")
            NativeExport("stale")
        }
        assertTrue(db.exports().entries().isEmpty())
        assertEquals("response-2", load())
    }

    @Test
    fun `server removal and reconnect during fetch cannot resurrect an export`() = runBlocking {
        cache.connection("s", true)
        cache.load("s", "chat") {
            cache.clearServer("s")
            NativeExport("removed")
        }
        assertTrue(db.exports().entries().isEmpty())
        cache.connection("s", true)
        cache.load("s", "chat") {
            cache.connection("s", false)
            cache.connection("s", true)
            NativeExport("old connection")
        }
        assertTrue(db.exports().entries().isEmpty())
        assertEquals("response-1", load())
    }

    private suspend fun write(server: String, session: String, text: String, max: Long = 100, keep: Int = 2, ttl: Long = 100) =
        db.exports().write(server, session, "r", text, ++now, max, keep, ttl)

    private suspend fun read(server: String, session: String, max: Long = 100, keep: Int = 2, ttl: Long = 100): String? =
        db.exports().read(server, session, "r", ++now, max, keep, ttl)

    @Test
    fun `LRU count byte budget expiry and oversized bypass`() = runBlocking {
        write("s", "a", "a".repeat(40))
        write("s", "b", "b".repeat(40))
        read("s", "a")
        write("s", "c", "c".repeat(40))
        assertNull(read("s", "b"))
        assertEquals(2, db.exports().entries().size)
        write("other", "d", "d".repeat(40))
        assertTrue(db.exports().entries().sumOf { it.bytes } <= 100)
        write("s", "huge", "x".repeat(101))
        assertNull(read("s", "huge"))
        now += 101
        assertNull(read("other", "d"))
        assertTrue(db.exports().entries().isEmpty())
    }

    @Test
    fun `large unicode documents round trip in small chunks and corruption recovers`() = runBlocking {
        val text = "native 😀 metadata\n".repeat(200_000)
        val max = 16L * 1024 * 1024
        write("s", "a", text, max)
        assertEquals(text, read("s", "a", max))
        assertTrue(db.exports().chunks("s", "a").all { it.text.encodeToByteArray().size < 1024 * 1024 })
        db.exports().putChunks(listOf(ExportChunk("s", "a", 0, "broken")))
        assertNull(read("s", "a", max))
        assertTrue(db.exports().chunks("s", "a").isEmpty())
        write("s", "a", "ok")
        assertEquals("ok", read("s", "a"))
    }
}
