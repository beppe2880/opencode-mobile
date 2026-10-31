package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.vcs.RemoteVcsRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteVcsRepositoryTest {
    private val server = MockWebServer()
    private lateinit var vcs: RemoteVcsRepository

    @Before
    fun setUp() {
        server.start()
        vcs = RemoteVcsRepository(TestGraph.activeOn(server).clients, warmUpRetries = 2, warmUpDelayMs = 0)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `branch of the project is asked again while the server is still opening the repository`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"location":{"directory":"/repo"},"data":{"branch":{}}}"""))
        server.enqueue(
            MockResponse().setBody(
                """{"location":{"directory":"/repo"},"data":{"provider":"git","branch":{"current":"feat","default":"main"}}}""",
            ),
        )
        assertEquals(VcsBranch("feat", "main"), vcs.branch("/repo").getOrThrow())
        repeat(2) {
            val url = server.takeRequest().requestUrl!!
            assertEquals("/api/vcs", url.encodedPath)
            assertEquals("/repo", url.queryParameter("location[directory]"))
        }
    }

    @Test
    fun `a directory without a provider is not a repository`() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setBody("""{"location":{"directory":"/tmp"},"data":{"branch":{}}}""")) }
        assertNull(vcs.branch("/tmp").getOrThrow())
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `changed files come from the working copy status`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"location":{"directory":"/repo"},"data":[
                    {"file":"a.kt","additions":1,"deletions":0,"status":"added"},
                    {"file":"b.kt","additions":2,"deletions":3,"status":"modified"}
                ]}""",
            ),
        )
        assertEquals(listOf("a.kt", "b.kt"), vcs.changedFiles("/repo").getOrThrow())
        val url = server.takeRequest().requestUrl!!
        assertEquals("/api/vcs/status", url.encodedPath)
        assertEquals("/repo", url.queryParameter("location[directory]"))
    }

    @Test
    fun `failures stay typed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"_tag":"UnauthorizedError","message":"Authentication required"}"""))
        assertTrue(vcs.changedFiles("/repo").exceptionOrNull() is OpenCodeError.Unauthorized)
    }
}
