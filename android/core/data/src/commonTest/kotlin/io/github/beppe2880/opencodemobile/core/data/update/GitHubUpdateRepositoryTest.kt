package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.data.update.GitHubUpdateRepository.Companion.CHECK_INTERVAL_MS
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubUpdateRepositoryTest {

    private val prefs = MapSettings()
    private var now = 1_000_000L
    private var latest: AppRelease? = release("1.0.0")
    private var failing = false
    private var fetches = 0

    private fun repository() = GitHubUpdateRepository(
        fetchLatest = {
            fetches++
            if (failing) throw IOException("offline")
            latest
        },
        prefs = prefs,
        now = { now },
    )

    private fun release(version: String) = AppRelease(version, "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version")

    @Test
    fun githubIsAskedAtMostOnceADayAlsoAcrossRestarts() = runTest {
        assertEquals(release("1.0.0"), repository().availableUpdate("0.9.0"))
        assertEquals(1, fetches)

        latest = release("1.1.0")
        now += CHECK_INTERVAL_MS - 1
        // A new process within the day reuses the release found by the last check.
        assertEquals(release("1.0.0"), repository().availableUpdate("0.9.0"))
        assertEquals(1, fetches)

        now += 1
        assertEquals(release("1.1.0"), repository().availableUpdate("0.9.0"))
        assertEquals(2, fetches)
    }

    @Test
    fun aClockMovedBackChecksAgain() = runTest {
        val repository = repository()
        repository.availableUpdate("0.9.0")
        now -= 1
        repository.availableUpdate("0.9.0")
        assertEquals(2, fetches)
    }

    @Test
    fun aFailedCheckIsRetriedAtTheNextLaunchAndKeepsTheLastRelease() = runTest {
        val repository = repository()
        repository.availableUpdate("0.9.0")
        now += CHECK_INTERVAL_MS
        failing = true

        assertEquals(release("1.0.0"), repository.availableUpdate("0.9.0"))
        assertEquals(release("1.0.0"), repository.availableUpdate("0.9.0"))
        assertEquals(3, fetches)

        failing = false
        latest = release("1.1.0")
        assertEquals(release("1.1.0"), repository.availableUpdate("0.9.0"))
        assertEquals(4, fetches)
        assertEquals(release("1.1.0"), repository.availableUpdate("0.9.0"))
        assertEquals(4, fetches)
    }

    @Test
    fun onlyReleasesNewerThanTheInstalledVersionAreOffered() = runTest {
        val repository = repository()
        assertNull(repository.availableUpdate("1.0.0"))
        assertNull(repository.availableUpdate("1.0.0-kmp"))
        assertNull(repository.availableUpdate("1.2.0"))
        assertEquals(release("1.0.0"), repository.availableUpdate("0.9.0-kmp"))
    }

    @Test
    fun buildsWithoutAVersionNeverAskGithub() = runTest {
        assertNull(repository().availableUpdate("dev"))
        assertEquals(0, fetches)
    }

    @Test
    fun noPublishedReleaseClearsTheStoredOne() = runTest {
        val repository = repository()
        repository.availableUpdate("0.9.0")
        latest = null
        now += CHECK_INTERVAL_MS
        assertNull(repository.availableUpdate("0.9.0"))
    }

    @Test
    fun aDismissedReleaseStaysHiddenUntilANewerOne() = runTest {
        val repository = repository()
        repository.dismiss(repository.availableUpdate("0.9.0")!!)
        assertNull(repository.availableUpdate("0.9.0"))
        assertNull(repository().availableUpdate("0.9.0"))

        latest = release("1.0.1")
        now += CHECK_INTERVAL_MS
        assertEquals(release("1.0.1"), repository.availableUpdate("0.9.0"))
    }
}
