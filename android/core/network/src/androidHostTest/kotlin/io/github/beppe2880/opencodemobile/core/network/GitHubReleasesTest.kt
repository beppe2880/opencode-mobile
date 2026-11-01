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
    private val releases = GitHubReleases(releasesUrl = server.url("/repos/beppe2880/opencode-mobile/releases").toString())

    @After
    fun tearDown() = server.shutdown()

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    private fun release(tag: String, prerelease: Boolean = false, draft: Boolean = false, url: String = PAGE + tag, body: String = "notes $tag") =
        """{"tag_name":"$tag","name":"$tag","html_url":"$url","prerelease":$prerelease,"draft":$draft,"assets":[],"body":"$body"}"""

    @Test
    fun `recent releases are read in one request without credentials, newest first`() = runBlocking {
        respond("[${release("v0.8.0")},${release("v0.10.0")},${release("v0.9.0")}]")

        assertEquals(
            listOf(
                AppRelease("0.10.0", PAGE + "v0.10.0", "notes v0.10.0"),
                AppRelease("0.9.0", PAGE + "v0.9.0", "notes v0.9.0"),
                AppRelease("0.8.0", PAGE + "v0.8.0", "notes v0.8.0"),
            ),
            releases.recent(),
        )
        assertEquals(1, server.requestCount)
        val request = server.takeRequest()
        assertEquals("/repos/beppe2880/opencode-mobile/releases?per_page=${GitHubReleases.RECENT_COUNT}", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals("application/vnd.github+json", request.getHeader("Accept"))
    }

    @Test
    fun `drafts, pre-releases, foreign pages and other tags are skipped`() = runBlocking {
        respond(
            "[" + listOf(
                release("v1.3.0", prerelease = true),
                release("v1.2.0", draft = true),
                release("v1.1.0", url = "https://example.com/v1.1.0"),
                release("nightly"),
                release("v1.0.0"),
            ).joinToString(",") + "]",
        )

        assertEquals(listOf("1.0.0"), releases.recent().map { it.version })
    }

    @Test
    fun `a release without notes has empty notes`() = runBlocking {
        respond("""[{"tag_name":"v1.0.0","html_url":"${PAGE}v1.0.0","body":null}]""")
        assertEquals(listOf(AppRelease("1.0.0", PAGE + "v1.0.0", "")), releases.recent())
    }

    @Test
    fun `no release yet is not an error`() = runBlocking {
        respond("[]")
        assertEquals(emptyList<AppRelease>(), releases.recent())
        respond("""{"message":"Not Found"}""", code = 404)
        assertEquals(emptyList<AppRelease>(), releases.recent())
    }

    @Test(expected = IOException::class)
    fun `a rate limit is a failure`(): Unit = runBlocking {
        respond("""{"message":"API rate limit exceeded"}""", code = 403)
        releases.recent()
    }

    @Test(expected = IOException::class)
    fun `an unexpected body is a failure`(): Unit = runBlocking {
        respond("""{"unexpected":true}""")
        releases.recent()
    }

    private companion object {
        const val PAGE = "https://github.com/beppe2880/opencode-mobile/releases/tag/"
    }
}
