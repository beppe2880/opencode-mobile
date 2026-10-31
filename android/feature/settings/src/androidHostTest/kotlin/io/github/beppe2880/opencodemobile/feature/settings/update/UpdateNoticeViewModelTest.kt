package io.github.beppe2880.opencodemobile.feature.settings.update

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.FakeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class UpdateNoticeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val release = AppRelease("1.0.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v1.0.0")
    private val updates = FakeUpdates(release)
    private val settings = FakeSettings()

    private fun vm(target: UpdateTarget = UpdateTarget.Android) = UpdateNoticeViewModel(updates, settings, AppInfo("0.9.0", "2.0.27", target))

    @Test
    fun `a newer release is shown and opens its page`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(release, vm.state.value.release)
        assertEquals(UpdateActionKind.Download, vm.state.value.action?.kind)
        assertEquals(listOf("0.9.0"), updates.checkedWith)
        vm.onEvent(UpdateNoticeEvent.Open)
        assertEquals(UpdateNoticeEffect.OpenUrl(release.url), vm.effect.first())
    }

    @Test
    fun `a store page is opened instead of the release when the platform has one`() = runTest(main.testDispatcher) {
        val vm = vm(UpdateTarget(releaseHasDownload = false, storeUrl = "https://apps.apple.com/app/id1"))
        advanceUntilIdle()

        assertEquals(UpdateActionKind.Store, vm.state.value.action?.kind)
        vm.onEvent(UpdateNoticeEvent.Open)
        assertEquals(UpdateNoticeEffect.OpenUrl("https://apps.apple.com/app/id1"), vm.effect.first())
    }

    @Test
    fun `dismissing hides the release and remembers it`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(UpdateNoticeEvent.Dismiss)
        advanceUntilIdle()

        assertNull(vm.state.value.release)
        assertNull(vm.state.value.action)
        assertEquals(listOf(release), updates.dismissed)
    }

    @Test
    fun `nothing is checked while the setting is off`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(checkForUpdates = false)
        val vm = vm()
        advanceUntilIdle()

        assertNull(vm.state.value.release)
        assertEquals(emptyList<String>(), updates.checkedWith)

        settings.settings.value = AppSettings(checkForUpdates = true)
        advanceUntilIdle()
        assertEquals(release, vm.state.value.release)

        settings.settings.value = AppSettings(checkForUpdates = false)
        advanceUntilIdle()
        assertNull(vm.state.value.release)
    }
}

private class FakeUpdates(private val release: AppRelease?) : UpdateRepository {
    val checkedWith = mutableListOf<String>()
    val dismissed = mutableListOf<AppRelease>()

    override suspend fun availableUpdate(installedVersion: String): AppRelease? {
        checkedWith += installedVersion
        return release?.takeIf { it !in dismissed }
    }

    override suspend fun dismiss(release: AppRelease) {
        dismissed += release
    }
}
