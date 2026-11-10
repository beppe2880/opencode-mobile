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
import kotlinx.coroutines.CompletableDeferred
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

    /** The session list's ViewModel, or with [automatic] false the one of Settings. */
    private fun vm(target: UpdateTarget = UpdateTarget.Android, automatic: Boolean = true) =
        UpdateNoticeViewModel(updates, settings, AppInfo("0.9.0", "2.0.27", target), session, automatic)

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
        // The window shows what changed at once.
        assertTrue(state.changelogVisible)
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
        // The window becomes a banner, as a reminder.
        assertTrue(vm.state.value.bannerVisible)

        // The session list is opened again in the same process: no sheet, the banner.
        val again = vm()
        advanceUntilIdle()
        assertEquals(release, again.state.value.release)
        assertFalse(again.state.value.sheetVisible)
        assertTrue(again.state.value.bannerVisible)

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
        // Closed with Later: the banner stays as a reminder.
        assertTrue(nextStart.state.value.bannerVisible)
        val again = vm()
        advanceUntilIdle()
        assertFalse(again.state.value.sheetVisible)
        assertTrue(again.state.value.bannerVisible)
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

    @Test
    fun `tapping the banner opens the sheet with the changelog folded`() = runTest(main.testDispatcher) {
        style(UpdateNoticeStyle.Banner)
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(UpdateNoticeEvent.ShowSheet)
        assertTrue(vm.state.value.sheetVisible)
        assertFalse(vm.state.value.bannerVisible)
        assertFalse(vm.state.value.changelogVisible)

        vm.onEvent(UpdateNoticeEvent.ToggleChangelog)
        assertTrue(vm.state.value.changelogVisible)

        vm.onEvent(UpdateNoticeEvent.Later)
        assertFalse(vm.state.value.sheetVisible)
        assertTrue(vm.state.value.bannerVisible)
    }

    @Test
    fun `settings never looks for updates on its own`() = runTest(main.testDispatcher) {
        val settingsVm = vm(automatic = false)
        advanceUntilIdle()

        assertNull(settingsVm.state.value.release)
        assertFalse(settingsVm.state.value.sheetVisible)
        assertEquals(emptyList<String>(), updates.checkedWith)
        assertFalse(session.shown)
    }

    @Test
    fun `check now says when the installed version is the newest`() = runTest(main.testDispatcher) {
        updates.now = Result.success(null)
        val gate = CompletableDeferred<Unit>()
        updates.gate = gate
        val vm = vm(automatic = false)
        advanceUntilIdle()

        vm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()
        assertEquals(UpdateCheck.Checking, vm.state.value.check)
        // A second tap while checking does not ask again.
        vm.onEvent(UpdateNoticeEvent.CheckNow)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(UpdateCheck.UpToDate, vm.state.value.check)
        assertFalse(vm.state.value.sheetVisible)
        assertEquals(listOf("0.9.0"), updates.checkedNowWith)
    }

    @Test
    fun `check now reports a failure`() = runTest(main.testDispatcher) {
        updates.now = Result.failure(IllegalStateException("offline"))
        val vm = vm(automatic = false)
        advanceUntilIdle()

        vm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()
        assertEquals(UpdateCheck.Failed, vm.state.value.check)
        assertFalse(vm.state.value.sheetVisible)

        // Tried again once back online.
        updates.now = Result.success(null)
        vm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()
        assertEquals(UpdateCheck.UpToDate, vm.state.value.check)
    }

    @Test
    fun `check now opens the sheet for a skipped version and the list shows it after Later`() = runTest(main.testDispatcher) {
        style(UpdateNoticeStyle.Banner)
        updates.dismissed += release
        val list = vm()
        val settingsVm = vm(automatic = false)
        advanceUntilIdle()
        assertFalse(list.state.value.bannerVisible)

        settingsVm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()
        val state = settingsVm.state.value
        assertEquals(UpdateCheck.Idle, state.check)
        assertTrue(state.sheetVisible)
        assertEquals(release, state.release)
        assertFalse(state.changelogVisible)
        assertEquals(listOf("1.0.0", "0.9.5"), state.changes.map { it.version })

        settingsVm.onEvent(UpdateNoticeEvent.Later)
        assertTrue(list.state.value.bannerVisible)
        assertFalse(list.state.value.sheetVisible)
    }

    @Test
    fun `with the notice off check now still opens the sheet but no banner follows`() = runTest(main.testDispatcher) {
        style(UpdateNoticeStyle.Off)
        val list = vm()
        val settingsVm = vm(automatic = false)
        advanceUntilIdle()

        settingsVm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()
        assertTrue(settingsVm.state.value.sheetVisible)
        assertEquals(emptyList<String>(), updates.checkedWith)

        settingsVm.onEvent(UpdateNoticeEvent.Later)
        assertFalse(list.state.value.bannerVisible)
        assertFalse(list.state.value.sheetVisible)
    }

    @Test
    fun `an update found by check now does not open the window again`() = runTest(main.testDispatcher) {
        val settingsVm = vm(automatic = false)
        advanceUntilIdle()
        settingsVm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()
        assertTrue(session.shown)

        val list = vm()
        advanceUntilIdle()
        assertFalse(list.state.value.sheetVisible)
        assertTrue(list.state.value.bannerVisible)
    }

    @Test
    fun `skipping the version found by check now hides the banner too`() = runTest(main.testDispatcher) {
        style(UpdateNoticeStyle.Banner)
        val list = vm()
        val settingsVm = vm(automatic = false)
        advanceUntilIdle()
        settingsVm.onEvent(UpdateNoticeEvent.CheckNow)
        advanceUntilIdle()

        settingsVm.onEvent(UpdateNoticeEvent.Dismiss)
        advanceUntilIdle()
        assertFalse(settingsVm.state.value.sheetVisible)
        assertEquals(listOf(release), updates.dismissed)
        assertNull(list.state.value.release)
        assertFalse(list.state.value.bannerVisible)
    }
}

private class FakeUpdates(var update: AppUpdate?) : UpdateRepository {
    val checkedWith = mutableListOf<String>()
    val checkedNowWith = mutableListOf<String>()
    val dismissed = mutableListOf<AppRelease>()

    /** What [checkNow] answers; by default [update], skipped or not. */
    var now: Result<AppUpdate?>? = null

    /** When set, [checkNow] waits for it. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun availableUpdate(installedVersion: String): AppUpdate? {
        checkedWith += installedVersion
        return update?.takeIf { it.latest !in dismissed }
    }

    override suspend fun checkNow(installedVersion: String): Result<AppUpdate?> {
        checkedNowWith += installedVersion
        gate?.await()
        return now ?: Result.success(update)
    }

    override suspend fun dismiss(release: AppRelease) {
        dismissed += release
    }
}
