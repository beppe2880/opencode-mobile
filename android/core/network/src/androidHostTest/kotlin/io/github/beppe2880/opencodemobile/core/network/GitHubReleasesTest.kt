package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class GitHubReleasesTest {

    private val server = MockWebServer().apply { start() }
    private val releases = GitHubReleases(latestUrl = server.url("/repos/beppe2880/opencode-mobile/releases/latest").toString())

    @After
    fun tearDown() = server.shutdown()

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    private fun release(tag: String, prerelease: Boolean = false, draft: Boolean = false, url: String = PAGE + tag) =
        """{"tag_name":"$tag","name":"$tag","html_url":"$url","prerelease":$prerelease,"draft":$draft,"assets":[],"body":"notes"}"""

    @Test
    fun `the latest release is read without credentials`() = runBlocking {
        respond(release("v1.0.0"))

        assertEquals(AppRelease("1.0.0", PAGE + "v1.0.0"), releases.latest())
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals("application/vnd.github+json", request.getHeader("Accept"))
    }

    @Test
    fun `drafts, pre-releases and foreign pages are skipped`() = runBlocking {
        respond(release("v1.0.0", prerelease = true))
        respond(release("v1.0.0", draft = true))
        respond(release("v1.0.0", url = "https://example.com/v1.0.0"))

        assertNull(releases.latest())
        assertNull(releases.latest())
        assertNull(releases.latest())
    }

    @Test
    fun `no release yet is not an error`() = runBlocking {
        respond("""{"message":"Not Found"}""", code = 404)
        assertNull(releases.latest())
    }

    @Test(expected = IOException::class)
    fun `a rate limit is a failure`(): Unit = runBlocking {
        respond("""{"message":"API rate limit exceeded"}""", code = 403)
        releases.latest()
    }

    @Test(expected = IOException::class)
    fun `an unexpected body is a failure`(): Unit = runBlocking {
        respond("""{"unexpected":true}""")
        releases.latest()
    }

    private companion object {
        const val PAGE = "https://github.com/beppe2880/opencode-mobile/releases/tag/"
    }
}
