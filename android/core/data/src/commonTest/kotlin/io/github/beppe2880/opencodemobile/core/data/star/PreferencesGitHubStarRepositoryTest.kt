package io.github.beppe2880.opencodemobile.core.data.star

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.data.star.PreferencesGitHubStarRepository.Companion.DAY_MS
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarChoice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferencesGitHubStarRepositoryTest {

    private val prefs = MapSettings()
    private var now = 20_000 * DAY_MS

    private fun repository() = PreferencesGitHubStarRepository(prefs, now = { now })

    /** Opens the app on [days] different days, a day apart, starting today. */
    private suspend fun PreferencesGitHubStarRepository.useOn(days: Int) {
        repeat(days) {
            if (it > 0) now += DAY_MS
            recordUse()
        }
    }

    @Test
    fun bannerShowsWhenUsedOnEnoughDaysAWeekAfterTheFirst() = runTest {
        val repo = repository()
        repo.useOn(4)
        assertFalse(repo.state.value.bannerVisible, "four days in a row are not a week yet")
        now += 4 * DAY_MS
        repo.recordUse()
        assertTrue(repo.state.value.bannerVisible)
    }

    @Test
    fun anUpdateFromAVersionWithoutTheInvitationShowsTheBannerAtOnce() = runTest {
        prefs.putString("theme", "Dark")
        val repo = repository()
        repo.recordUse()
        assertTrue(repo.state.value.bannerVisible)
        now += DAY_MS
        val nextRun = repository()
        nextRun.recordUse()
        assertFalse(nextRun.state.value.bannerVisible)
    }

    @Test
    fun aNewInstallWaitsEvenWhenItWritesSettingsLater() = runTest {
        val repo = repository()
        prefs.putString("theme", "Dark")
        repo.recordUse()
        assertFalse(repo.state.value.bannerVisible)
        now += DAY_MS
        repository().apply {
            recordUse()
            assertFalse(state.value.bannerVisible)
        }
    }

    @Test
    fun openingTheAppManyTimesOnOneDayCountsOneDay() = runTest {
        val repo = repository()
        repeat(10) { repo.recordUse() }
        now += 10 * DAY_MS
        repo.recordUse()
        assertFalse(repo.state.value.bannerVisible)
    }

    @Test
    fun bannerIsShownInOneRunOnlyEvenWhenIgnored() = runTest {
        repository().apply {
            useOn(4)
            now += 4 * DAY_MS
            recordUse()
            assertTrue(state.value.bannerVisible)
        }
        now += DAY_MS
        val nextRun = repository()
        nextRun.recordUse()
        assertFalse(nextRun.state.value.bannerVisible)
        assertEquals(GitHubStarChoice.None, nextRun.state.value.choice)
    }

    @Test
    fun starringHidesTheBannerAndIsKept() = runTest {
        val repo = repository()
        repo.useOn(4)
        now += 4 * DAY_MS
        repo.recordUse()
        repo.starred()
        assertFalse(repo.state.value.bannerVisible)
        assertEquals(GitHubStarChoice.Starred, repository().state.value.choice)
    }

    @Test
    fun closingTheBannerIsKeptAndNeverBringsItBack() = runTest {
        val repo = repository()
        repo.useOn(4)
        now += 4 * DAY_MS
        repo.recordUse()
        repo.dismissBanner()
        assertFalse(repo.state.value.bannerVisible)
        assertEquals(GitHubStarChoice.Dismissed, repository().state.value.choice)
    }

    @Test
    fun aStarLeftElsewhereIsNotUndoneByClosingTheBanner() = runTest {
        val repo = repository()
        repo.starred()
        repo.dismissBanner()
        assertEquals(GitHubStarChoice.Starred, repo.state.value.choice)
    }

    @Test
    fun afterAStarTheBannerNeverShows() = runTest {
        val repo = repository()
        repo.starred()
        repo.useOn(4)
        now += 10 * DAY_MS
        repo.recordUse()
        assertFalse(repo.state.value.bannerVisible)
    }
}
