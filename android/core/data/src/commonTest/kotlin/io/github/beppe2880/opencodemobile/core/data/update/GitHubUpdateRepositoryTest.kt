package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.data.update.GitHubUpdateRepository.Companion.CHECK_INTERVAL_MS
import io.github.beppe2880.opencodemobile.core.data.update.GitHubUpdateRepository.Companion.KEY_RELEASES
import io.github.beppe2880.opencodemobile.core.data.update.GitHubUpdateRepository.Companion.KEY_URL
import io.github.beppe2880.opencodemobile.core.data.update.GitHubUpdateRepository.Companion.KEY_VERSION
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppUpdate
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubUpdateRepositoryTest {

    private val prefs = MapSettings()
    private var now = 1_000_000L
    private var published: List<AppRelease> = listOf(release("1.0.0"))
    private var failing = false
    private var fetches = 0

    private fun repository() = GitHubUpdateRepository(
        fetchReleases = {
            fetches++
            if (failing) throw IOException("offline")
            published
        },
        prefs = prefs,
        now = { now },
    )

    private fun release(version: String) =
        AppRelease(version, "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version", "### Features\n\n* Version $version")

    private fun update(vararg versions: String) = AppUpdate(release(versions.first()), versions.map(::release))

    @Test
    fun githubIsAskedAtMostOnceADayAlsoAcrossRestarts() = runTest {
        assertEquals(update("1.0.0"), repository().availableUpdate("0.9.0"))
        assertEquals(1, fetches)

        published = listOf(release("1.1.0"), release("1.0.0"))
        now += CHECK_INTERVAL_MS - 1
        // A new process within the day reuses the releases found by the last check, with their notes.
        assertEquals(update("1.0.0"), repository().availableUpdate("0.9.0"))
        assertEquals(1, fetches)

        now += 1
        assertEquals(update("1.1.0", "1.0.0"), repository().availableUpdate("0.9.0"))
        assertEquals(2, fetches)
    }

    @Test
    fun everyReleaseSinceTheInstalledOneComesFromOneRequestNewestFirst() = runTest {
        published = listOf(release("0.8.0"), release("1.0.0"), release("0.9.0"), release("0.7.0"))

        assertEquals(update("1.0.0", "0.9.0", "0.8.0"), repository().availableUpdate("0.7.0"))
        assertEquals(1, fetches)
        assertEquals(update("1.0.0", "0.9.0"), repository().availableUpdate("0.8.0"))
        assertEquals(1, fetches)
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
    fun aFailedCheckIsRetriedAtTheNextLaunchAndKeepsTheLastReleases() = runTest {
        val repository = repository()
        repository.availableUpdate("0.9.0")
        now += CHECK_INTERVAL_MS
        failing = true

        assertEquals(update("1.0.0"), repository.availableUpdate("0.9.0"))
        assertEquals(update("1.0.0"), repository.availableUpdate("0.9.0"))
        assertEquals(3, fetches)

        failing = false
        published = listOf(release("1.1.0"))
        assertEquals(update("1.1.0"), repository.availableUpdate("0.9.0"))
        assertEquals(4, fetches)
        assertEquals(update("1.1.0"), repository.availableUpdate("0.9.0"))
        assertEquals(4, fetches)
    }

    @Test
    fun offlineWithoutAnyCheckYetHasNoUpdate() = runTest {
        failing = true
        assertNull(repository().availableUpdate("0.9.0"))
    }

    @Test
    fun onlyReleasesNewerThanTheInstalledVersionAreOffered() = runTest {
        val repository = repository()
        assertNull(repository.availableUpdate("1.0.0"))
        assertNull(repository.availableUpdate("1.0.0-kmp"))
        assertNull(repository.availableUpdate("1.2.0"))
        assertEquals(update("1.0.0"), repository.availableUpdate("0.9.0-kmp"))
    }

    @Test
    fun buildsWithoutAVersionNeverAskGithub() = runTest {
        assertNull(repository().availableUpdate("dev"))
        assertEquals(0, fetches)
    }

    @Test
    fun noPublishedReleaseClearsTheStoredOnes() = runTest {
        val repository = repository()
        repository.availableUpdate("0.9.0")
        published = emptyList()
        now += CHECK_INTERVAL_MS
        assertNull(repository.availableUpdate("0.9.0"))
        assertNull(prefs.getStringOrNull(KEY_RELEASES))
    }

    @Test
    fun aDismissedReleaseStaysHiddenUntilANewerOne() = runTest {
        val repository = repository()
        repository.dismiss(repository.availableUpdate("0.9.0")!!.latest)
        assertNull(repository.availableUpdate("0.9.0"))
        assertNull(repository().availableUpdate("0.9.0"))

        published = listOf(release("1.0.1"), release("1.0.0"))
        now += CHECK_INTERVAL_MS
        // The skipped version is still listed: it was never installed.
        assertEquals(update("1.0.1", "1.0.0"), repository.availableUpdate("0.9.0"))
    }

    @Test
    fun theReleaseSavedByTheOlderAppIsKeptUntilTheNextCheck() = runTest {
        prefs.putLong(GitHubUpdateRepository.KEY_CHECKED_AT, now)
        prefs.putString(KEY_VERSION, "1.0.0")
        prefs.putString(KEY_URL, "https://github.com/beppe2880/opencode-mobile/releases/tag/v1.0.0")

        val update = repository().availableUpdate("0.9.0")
        assertEquals(AppRelease("1.0.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v1.0.0"), update?.latest)
        assertEquals(0, fetches)

        now += CHECK_INTERVAL_MS
        repository().availableUpdate("0.9.0")
        assertNull(prefs.getStringOrNull(KEY_VERSION))
        assertNull(prefs.getStringOrNull(KEY_URL))
    }

    @Test
    fun corruptStoredReleasesCountAsNone() = runTest {
        prefs.putLong(GitHubUpdateRepository.KEY_CHECKED_AT, now)
        prefs.putString(KEY_RELEASES, "{not json")
        assertNull(repository().availableUpdate("0.9.0"))
    }
}
