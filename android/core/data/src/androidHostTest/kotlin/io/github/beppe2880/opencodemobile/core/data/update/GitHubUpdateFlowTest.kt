package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNote
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNotes
import io.github.beppe2880.opencodemobile.core.network.GitHubReleases
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The update check against a GitHub stand-in: one request brings every release the user missed. */
class GitHubUpdateFlowTest {

    private val server = MockWebServer().apply { start() }
    private val prefs = MapSettings()
    private var now = 1_000_000L
    private val repository = GitHubUpdateRepository(
        fetchReleases = GitHubReleases(releasesUrl = server.url("/repos/beppe2880/opencode-mobile/releases").toString())::recent,
        prefs = prefs,
        now = { now },
    )

    @After
    fun tearDown() = server.shutdown()

    private fun release(version: String, change: String) =
        """{"tag_name":"v$version","html_url":"$PAGE$version","draft":false,"prerelease":false,""" +
            """"body":"## [$version](https://github.com/x/compare) (2026-10-27)\n\n\n### Features\n\n* **chat:** $change ([#1](https://github.com/x/issues/1)) ([abcdef1](https://github.com/x/commit/abcdef1))\n"}"""

    @Test
    fun `releases since the installed one come from one request a day, offline keeps them`() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json")
                .setBody("[${release("0.10.0", "Ten")},${release("0.9.0", "Nine")},${release("0.8.0", "Eight")}]"),
        )

        val update = repository.availableUpdate("0.8.0")!!
        assertEquals("0.10.0", update.latest.version)
        assertEquals(listOf("0.10.0", "0.9.0"), update.releases.map { it.version })
        assertEquals(
            listOf(listOf(ReleaseNote("Ten", "chat")), listOf(ReleaseNote("Nine", "chat"))),
            update.releases.map { ReleaseNotes.parse(it.notes).single().items },
        )

        // Later in the day, and once GitHub is unreachable, nothing more is asked or lost.
        assertEquals(update, repository.availableUpdate("0.8.0"))
        assertEquals(1, server.requestCount)
        now += GitHubUpdateRepository.CHECK_INTERVAL_MS
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(update, repository.availableUpdate("0.8.0"))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `no release yet means no update`() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("[]"))
        assertNull(repository.availableUpdate("0.8.0"))
    }

    private companion object {
        const val PAGE = "https://github.com/beppe2880/opencode-mobile/releases/tag/v"
    }
}
