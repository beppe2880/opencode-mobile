package io.github.beppe2880.opencodemobile.feature.settings.update

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.AppUpdate
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNote
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSectionKind
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.FakeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateNoticeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val release = release("1.0.0", "### Features\n\n* **chat:** Newest ([#2](https://x/2))")
    private val older = release("0.9.5", "### Bug fixes\n\n* Older fix")
    private val updates = FakeUpdates(AppUpdate(release, listOf(release, older)))
    private val settings = FakeSettings()
    private val session = UpdateSheetSession()

    private fun release(version: String, notes: String) =
        AppRelease(version, "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version", notes)

    private fun vm(target: UpdateTarget = UpdateTarget.Android) =
        UpdateNoticeViewModel(updates, settings, AppInfo("0.9.0", "2.0.27", target), session)

    private fun style(style: UpdateNoticeStyle) {
        settings.settings.value = AppSettings(updateNotice = style)
    }

    @Test
    fun `by default a newer release opens the sheet with what changed in every version`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.sheetVisible)
        assertFalse(state.bannerVisible)
        assertEquals(release, state.release)
        assertEquals("0.9.0", state.installedVersion)
        assertEquals(listOf("1.0.0", "0.9.5"), state.changes.map { it.version })
        assertEquals(ReleaseSectionKind.Features, state.changes[0].sections.single().kind)
        assertEquals(listOf(ReleaseNote("Newest", "chat")), state.changes[0].sections.single().items)
        assertEquals(listOf(ReleaseNote("Older fix")), state.changes[1].sections.single().items)
        assertEquals(listOf("0.9.0"), updates.checkedWith)
    }

    @Test
    fun `the main action opens the release page and closes the sheet`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(UpdateActionKind.Download, vm.state.value.action?.kind)
        vm.onEvent(UpdateNoticeEvent.Open)
        assertEquals(UpdateNoticeEffect.OpenUrl(release.url), vm.effect.first())
        assertFalse(vm.state.value.sheetVisible)
        assertEquals(emptyList<AppRelease>(), updates.dismissed)
    }

    @Test
    fun `iOS opens the release notes, a store page wins when the platform has one`() = runTest(main.testDispatcher) {
        val ios = vm(UpdateTarget.Ios)
        advanceUntilIdle()
        assertEquals(UpdateAction(UpdateActionKind.ReleaseNotes, release.url), ios.state.value.action)

        val store = vm(UpdateTarget(releaseHasDownload = false, storeUrl = "https://apps.apple.com/app/id1"))
        advanceUntilIdle()
        assertEquals(UpdateActionKind.Store, store.state.value.action?.kind)
    }

    @Test
    fun `later closes the sheet for this app run only`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(UpdateNoticeEvent.Later)
        assertFalse(vm.state.value.sheetVisible)
        assertEquals(emptyList<AppRelease>(), updates.dismissed)

        // The session list is opened again in the same process: no sheet.
        val again = vm()
        advanceUntilIdle()
        assertEquals(release, again.state.value.release)
        assertFalse(again.state.value.sheetVisible)

        // A new process (a new session holder) asks again.
        val nextStart = UpdateNoticeViewModel(updates, settings, AppInfo("0.9.0", "2.0.27", UpdateTarget.Android), UpdateSheetSession())
        advanceUntilIdle()
        assertTrue(nextStart.state.value.sheetVisible)
    }

    @Test
    fun `skip hides the release and remembers it`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(UpdateNoticeEvent.Dismiss)
        advanceUntilIdle()

        assertNull(vm.state.value.release)
        assertNull(vm.state.value.action)
        assertFalse(vm.state.value.sheetVisible)
        assertEquals(listOf(release), updates.dismissed)

        val nextStart = UpdateNoticeViewModel(updates, settings, AppInfo("0.9.0", "2.0.27", UpdateTarget.Android), UpdateSheetSession())
        advanceUntilIdle()
        assertNull(nextStart.state.value.release)
        assertFalse(nextStart.state.value.sheetVisible)
    }

    @Test
    fun `banner mode shows the banner and never the sheet`() = runTest(main.testDispatcher) {
        style(UpdateNoticeStyle.Banner)
        val vm = vm()
        advanceUntilIdle()

        assertTrue(vm.state.value.bannerVisible)
        assertFalse(vm.state.value.sheetVisible)
        assertFalse(session.shown)

        vm.onEvent(UpdateNoticeEvent.Dismiss)
        advanceUntilIdle()
        assertFalse(vm.state.value.bannerVisible)
        assertEquals(listOf(release), updates.dismissed)
    }

    @Test
    fun `nothing is checked while the notice is off`() = runTest(main.testDispatcher) {
        style(UpdateNoticeStyle.Off)
        val vm = vm()
        advanceUntilIdle()

        assertNull(vm.state.value.release)
        assertFalse(vm.state.value.sheetVisible)
        assertFalse(vm.state.value.bannerVisible)
        assertEquals(emptyList<String>(), updates.checkedWith)

        style(UpdateNoticeStyle.Banner)
        advanceUntilIdle()
        assertTrue(vm.state.value.bannerVisible)

        style(UpdateNoticeStyle.Off)
        advanceUntilIdle()
        assertNull(vm.state.value.release)
        assertFalse(vm.state.value.bannerVisible)
    }

    @Test
    fun `without notes the window falls back to the banner until the next check brings them`() = runTest(main.testDispatcher) {
        // As saved by the former app: version and page only.
        val bare = AppRelease(release.version, release.url)
        updates.update = AppUpdate(bare, listOf(bare, older.copy(notes = "")))
        val vm = vm()
        advanceUntilIdle()

        assertFalse(vm.state.value.sheetVisible)
        assertTrue(vm.state.value.bannerVisible)
        assertEquals(emptyList<VersionChanges>(), vm.state.value.changes)
        assertFalse(session.shown)

        // The next daily check, at a later start, stored the notes: the sheet shows, once.
        updates.update = AppUpdate(release, listOf(release, older))
        val nextStart = vm()
        advanceUntilIdle()
        assertTrue(nextStart.state.value.sheetVisible)
        assertFalse(nextStart.state.value.bannerVisible)
        nextStart.onEvent(UpdateNoticeEvent.Later)
        assertFalse(nextStart.state.value.bannerVisible)
        val again = vm()
        advanceUntilIdle()
        assertFalse(again.state.value.sheetVisible)
        assertFalse(again.state.value.bannerVisible)
    }

    @Test
    fun `versions without notes are left out of the sheet`() = runTest(main.testDispatcher) {
        updates.update = AppUpdate(release, listOf(release, older.copy(notes = "Nothing structured here.")))
        val vm = vm()
        advanceUntilIdle()

        assertTrue(vm.state.value.sheetVisible)
        assertEquals(listOf("1.0.0"), vm.state.value.changes.map { it.version })
    }

    @Test
    fun `no update, as in the demo or offline before any check, shows nothing`() = runTest(main.testDispatcher) {
        val vm = UpdateNoticeViewModel(FakeUpdates(null), settings, AppInfo("0.9.0", "2.0.27", UpdateTarget.Android), session)
        advanceUntilIdle()

        assertNull(vm.state.value.release)
        assertFalse(vm.state.value.sheetVisible)
        assertFalse(vm.state.value.bannerVisible)
        // Nothing was shown, so a later start with an update still gets its sheet.
        assertFalse(session.shown)
        vm.onEvent(UpdateNoticeEvent.Open)
        vm.onEvent(UpdateNoticeEvent.Dismiss)
    }
}

private class FakeUpdates(var update: AppUpdate?) : UpdateRepository {
    val checkedWith = mutableListOf<String>()
    val dismissed = mutableListOf<AppRelease>()

    override suspend fun availableUpdate(installedVersion: String): AppUpdate? {
        checkedWith += installedVersion
        return update?.takeIf { it.latest !in dismissed }
    }

    override suspend fun dismiss(release: AppRelease) {
        dismissed += release
    }
}
