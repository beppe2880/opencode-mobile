package io.github.beppe2880.opencodemobile.feature.settings.star

import io.github.beppe2880.opencodemobile.core.domain.APP_REPOSITORY_URL
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarChoice
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.feature.settings.FakeGitHubStar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GitHubStarViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val star = FakeGitHubStar()

    @Test
    fun `the banner counts a day of use, the card does not`() = runTest(main.testDispatcher) {
        GitHubStarViewModel(star, countUse = false)
        advanceUntilIdle()
        assertEquals(0, star.uses)
        GitHubStarViewModel(star, countUse = true)
        advanceUntilIdle()
        assertEquals(1, star.uses)
    }

    @Test
    fun `the banner follows the repository`() = runTest(main.testDispatcher) {
        val vm = GitHubStarViewModel(star, countUse = true)
        star.showBanner()
        advanceUntilIdle()
        assertTrue(vm.state.value.bannerVisible)
    }

    @Test
    fun `star opens the repository and the banner thanks until its X`() = runTest(main.testDispatcher) {
        val vm = GitHubStarViewModel(star)
        star.showBanner()
        vm.onEvent(GitHubStarEvent.Star)
        assertEquals(GitHubStarEffect.OpenUrl(APP_REPOSITORY_URL), vm.effect.first())
        advanceUntilIdle()
        assertEquals(GitHubStarChoice.Starred, star.state.value.choice)
        assertTrue(vm.state.value.thanked)
        // It thanks until its X.
        assertTrue(vm.state.value.bannerVisible)
        vm.onEvent(GitHubStarEvent.DismissBanner)
        advanceUntilIdle()
        assertFalse(vm.state.value.bannerVisible)
        assertEquals(GitHubStarChoice.Starred, star.state.value.choice)
    }

    @Test
    fun `closing the banner dismisses it`() = runTest(main.testDispatcher) {
        val vm = GitHubStarViewModel(star)
        star.showBanner()
        vm.onEvent(GitHubStarEvent.DismissBanner)
        advanceUntilIdle()
        assertEquals(GitHubStarChoice.Dismissed, star.state.value.choice)
        assertFalse(vm.state.value.bannerVisible)
        assertFalse(vm.state.value.thanked)
    }
}
