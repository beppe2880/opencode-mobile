package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.data.update.GitHubWhatsNewRepository.Companion.CHECK_INTERVAL_MS
import io.github.beppe2880.opencodemobile.core.data.update.GitHubWhatsNewRepository.Companion.KEY_RELEASES
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubWhatsNewRepositoryTest {

    private val prefs = MapSettings()
    private var now = 1_000_000L
    private var published: List<AppRelease> = listOf(release("1.0.0"))
    private var failing = false
    private var fetches = 0

    private fun repository() = GitHubWhatsNewRepository(
        fetchReleases = {
            fetches++
            if (failing) throw IOException("offline")
            published
        },
        prefs = prefs,
        now = { now },
    )

    private fun release(version: String) = AppRelease(
        version = version,
        url = "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version",
        notes = "### Features\n\n* Version $version",
        publishedAt = 1_793_059_200_000,
    )

    @Test
    fun nothingIsKnownBeforeTheFirstRequest() {
        val history = repository().history.value
        assertEquals(emptyList(), history.releases)
        assertNull(history.seenVersion)
        assertNull(history.hiddenVersion)
        assertEquals(0, fetches)
    }

    @Test
    fun aSuccessfulRequestIsKeptForTheNextStartWithItsDates() = runTest {
        published = listOf(release("1.1.0"), release("1.0.0"))
        val repository = repository()

        assertEquals(Result.success(published), repository.refresh())
        assertEquals(published, repository.history.value.releases)

        // A new process, offline: the page still has the releases, notes and dates.
        failing = true
        val restarted = repository()
        assertEquals(published, restarted.history.value.releases)
        assertTrue(restarted.refresh().isFailure)
        assertEquals(published, restarted.history.value.releases)
    }

    @Test
    fun aFailedRequestKeepsTheLastReleases() = runTest {
        val repository = repository()
        repository.refresh()
        failing = true

        assertTrue(repository.refresh().isFailure)
        assertEquals(published, repository.history.value.releases)
        assertEquals(published, repository().history.value.releases)
    }

    @Test
    fun noPublishedReleaseClearsTheKeptOnes() = runTest {
        val repository = repository()
        repository.refresh()
        published = emptyList()

        assertEquals(Result.success(emptyList<AppRelease>()), repository.refresh())
        assertEquals(emptyList<AppRelease>(), repository.history.value.releases)
        assertNull(prefs.getStringOrNull(KEY_RELEASES))
    }

    @Test
    fun corruptKeptReleasesCountAsNone() {
        prefs.putString(KEY_RELEASES, "{not json")
        assertEquals(emptyList(), repository().history.value.releases)
    }

    @Test
    fun githubIsAskedForTheDotAtMostOnceADayAlsoAcrossRestarts() = runTest {
        repository().refreshIfDue()
        assertEquals(1, fetches)

        now += CHECK_INTERVAL_MS - 1
        repository().refreshIfDue()
        assertEquals(1, fetches)

        now += 1
        published = listOf(release("1.1.0"), release("1.0.0"))
        val repository = repository()
        repository.refreshIfDue()
        assertEquals(2, fetches)
        assertEquals("1.1.0", repository.history.value.latestVersion)
    }

    @Test
    fun aFailedDailyRequestIsRetriedAndOpeningThePageCountsAsTheDaysRequest() = runTest {
        val repository = repository()
        failing = true
        repository.refreshIfDue()
        repository.refreshIfDue()
        assertEquals(2, fetches)

        failing = false
        repository.refresh()
        repository.refreshIfDue()
        assertEquals(3, fetches)
    }

    @Test
    fun openingThePageMarksTheNewestReleaseSeenAcrossRestarts() = runTest {
        val repository = repository()
        // Nothing known yet: nothing to mark.
        repository.markSeen()
        assertNull(repository.history.value.seenVersion)

        repository.refresh()
        assertTrue(repository.history.value.hasUnseen)
        repository.markSeen()
        assertEquals("1.0.0", repository.history.value.seenVersion)
        assertFalse(repository.history.value.hasUnseen)
        assertFalse(repository().history.value.hasUnseen)

        published = listOf(release("1.1.0"), release("1.0.0"))
        repository.refresh()
        assertTrue(repository.history.value.hasUnseen)
    }

    @Test
    fun theHiddenButtonComesBackWithANewerReleaseAcrossRestarts() = runTest {
        val repository = repository()
        repository.refresh()
        repository.hide(installedVersion = "0.9.0")

        assertEquals("1.0.0", repository.history.value.hiddenVersion)
        assertFalse(repository.history.value.buttonVisible)
        assertFalse(repository().history.value.buttonVisible)

        published = listOf(release("1.1.0"), release("1.0.0"))
        val restarted = repository()
        restarted.refresh()
        assertTrue(restarted.history.value.buttonVisible)
    }

    @Test
    fun hiddenBeforeAnyReleaseIsKnownItWaitsForOneNewerThanTheInstalledVersion() = runTest {
        val repository = repository()
        repository.hide(installedVersion = "1.0.0")
        assertEquals("1.0.0", repository.history.value.hiddenVersion)
        assertFalse(repository.history.value.buttonVisible)

        repository.refresh()
        assertFalse(repository.history.value.buttonVisible)

        published = listOf(release("1.0.1"), release("1.0.0"))
        repository.refresh()
        assertTrue(repository.history.value.buttonVisible)
    }
}
