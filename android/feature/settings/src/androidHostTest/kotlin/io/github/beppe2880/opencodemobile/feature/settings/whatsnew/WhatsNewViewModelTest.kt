package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.WhatsNewHistory
import io.github.beppe2880.opencodemobile.core.domain.WhatsNewRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import io.github.beppe2880.opencodemobile.feature.settings.FakeSettings
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateAction
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateActionKind
import io.github.beppe2880.opencodemobile.feature.settings.update.UpdateTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class WhatsNewViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val v090 = release("0.9.0", day = 22)
    private val v100 = release("1.0.0", day = 27, notes = "### Features\n\n* **chat:** Newest ([#2]($ISSUE))\n\n### Bug fixes\n\n* A fix")
    private val v110 = release("1.1.0", day = 30)

    private val repository = FakeWhatsNew()
    private val settings = FakeSettings()

    /** Published at noon UTC of a [day] of October 2026. */
    private fun release(version: String, day: Int, notes: String = "### Features\n\n* Version $version") = AppRelease(
        version = version,
        url = "https://github.com/beppe2880/opencode-mobile/releases/tag/v$version",
        notes = notes,
        publishedAt = LocalDateTime(2026, 10, day, 12, 0).toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )

    private fun page(installed: String = "0.9.0", target: UpdateTarget = UpdateTarget.Android, zone: TimeZone = TimeZone.UTC) =
        WhatsNewViewModel(repository, AppInfo(installed, "2.0.27", target), zone = { zone })

    private fun button(installed: String = "0.9.0") = WhatsNewButtonViewModel(repository, settings, AppInfo(installed, "2.0.27", UpdateTarget.Android))

    @Test
    fun `the kept releases show at once, then the ones github answers with`() = runTest(main.testDispatcher) {
        repository.state.value = WhatsNewHistory(releases = listOf(v090))
        repository.published = listOf(v100, v090)
        repository.gate = CompletableDeferred()

        val vm = page()
        advanceUntilIdle()
        assertEquals(listOf("0.9.0"), vm.state.value.days.map { it.version })
        assertTrue(vm.state.value.loading)
        assertFalse(vm.state.value.failed)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.loading)
        assertFalse(state.failed)
        assertEquals(listOf("1.0.0", "0.9.0"), state.days.map { it.version })
        assertEquals(listOf(LocalDate(2026, 10, 27), LocalDate(2026, 10, 22)), state.days.map { it.date })
        assertEquals("0.9.0", state.installedVersion)
        assertEquals(1, repository.refreshes)
    }

    @Test
    fun `the days carry the changes by type, the installed mark and the update`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100, v090)

        val vm = page()
        advanceUntilIdle()

        val (newer, installed) = vm.state.value.days
        assertTrue(newer.updateAvailable)
        assertFalse(newer.installed)
        assertEquals(
            listOf(
                WhatsNewGroup(ChangeType.New, listOf(WhatsNewChange("Newest", "chat", ISSUE))),
                WhatsNewGroup(ChangeType.Fixed, listOf(WhatsNewChange("A fix"))),
            ),
            newer.groups,
        )
        assertTrue(installed.installed)
        assertFalse(installed.updateAvailable)
        assertEquals(UpdateAction(UpdateActionKind.Download, v100.url), vm.state.value.update)
    }

    @Test
    fun `days are the ones of the device time zone`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100)

        // Noon UTC is already the next day fourteen hours ahead.
        val vm = page(zone = UtcOffset(hours = 14).asTimeZone())
        advanceUntilIdle()

        assertEquals(listOf(LocalDate(2026, 10, 28)), vm.state.value.days.map { it.date })
    }

    @Test
    fun `without kept releases the page loads, then shows them`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100, v090)
        repository.gate = CompletableDeferred()

        val vm = page()
        advanceUntilIdle()
        assertTrue(vm.state.value.loading)
        assertTrue(vm.state.value.days.isEmpty())

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertEquals(2, vm.state.value.days.size)
    }

    @Test
    fun `a failure without kept releases is an error, retry loads them`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100, v090)
        repository.failing = true

        val vm = page()
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.days.isEmpty())

        repository.failing = false
        repository.gate = CompletableDeferred()
        vm.onEvent(WhatsNewEvent.Refresh)
        advanceUntilIdle()
        // The error makes room for the placeholders while it tries again.
        assertTrue(vm.state.value.loading)
        assertFalse(vm.state.value.failed)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.failed)
        assertFalse(vm.state.value.loading)
        assertEquals(listOf("1.0.0", "0.9.0"), vm.state.value.days.map { it.version })
        assertEquals(2, repository.refreshes)
    }

    @Test
    fun `offline the kept releases stay with a notice`() = runTest(main.testDispatcher) {
        repository.state.value = WhatsNewHistory(releases = listOf(v100, v090))
        repository.failing = true

        val vm = page()
        advanceUntilIdle()

        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.loading)
        assertEquals(listOf("1.0.0", "0.9.0"), vm.state.value.days.map { it.version })
    }

    @Test
    fun `refreshing twice asks github once at a time`() = runTest(main.testDispatcher) {
        repository.gate = CompletableDeferred()
        val vm = page()
        advanceUntilIdle()

        vm.onEvent(WhatsNewEvent.Refresh)
        vm.onEvent(WhatsNewEvent.Refresh)
        advanceUntilIdle()
        assertEquals(1, repository.refreshes)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        vm.onEvent(WhatsNewEvent.Refresh)
        advanceUntilIdle()
        assertEquals(2, repository.refreshes)
    }

    @Test
    fun `no published release is an empty page, not an error`() = runTest(main.testDispatcher) {
        val vm = page()
        advanceUntilIdle()

        assertFalse(vm.state.value.loading)
        assertFalse(vm.state.value.failed)
        assertTrue(vm.state.value.days.isEmpty())
        assertNull(vm.state.value.update)
    }

    @Test
    fun `opening the page clears the dot of the button`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100, v090)
        val button = button()
        advanceUntilIdle()
        assertTrue(button.state.value.visible)
        assertTrue(button.state.value.unseen)

        page()
        advanceUntilIdle()

        assertEquals("1.0.0", repository.state.value.seenVersion)
        assertFalse(button.state.value.unseen)
        assertTrue(button.state.value.visible)
        // Also at the next start.
        assertFalse(button().state.value.unseen)
    }

    @Test
    fun `a release that arrives while the page is open is seen too`() = runTest(main.testDispatcher) {
        repository.state.value = WhatsNewHistory(releases = listOf(v100, v090))
        repository.published = listOf(v110, v100, v090)
        repository.gate = CompletableDeferred()

        page()
        advanceUntilIdle()
        assertEquals("1.0.0", repository.state.value.seenVersion)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals("1.1.0", repository.state.value.seenVersion)
        assertFalse(repository.state.value.hasUnseen)
    }

    @Test
    fun `offline with nothing kept the dot has nothing to clear`() = runTest(main.testDispatcher) {
        repository.failing = true
        page()
        advanceUntilIdle()
        assertNull(repository.state.value.seenVersion)
    }

    @Test
    fun `a change opens its link on github and nothing else`() = runTest(main.testDispatcher) {
        val vm = page()
        advanceUntilIdle()

        vm.onEvent(WhatsNewEvent.OpenChange("https://example.com/beppe2880/opencode-mobile/issues/2"))
        vm.onEvent(WhatsNewEvent.OpenChange(ISSUE))
        assertEquals(WhatsNewEffect.OpenUrl(ISSUE), vm.effect.first())
    }

    @Test
    fun `update opens what the update sheet offers for the newest release`() = runTest(main.testDispatcher) {
        repository.published = listOf(v110, v100, v090)

        val android = page()
        advanceUntilIdle()
        assertEquals(listOf(true, true, false), android.state.value.days.map { it.updateAvailable })
        assertEquals(UpdateAction(UpdateActionKind.Download, v110.url), android.state.value.update)
        android.onEvent(WhatsNewEvent.Update)
        assertEquals(WhatsNewEffect.OpenUrl(v110.url), android.effect.first())

        val ios = page(target = UpdateTarget.Ios)
        advanceUntilIdle()
        assertEquals(UpdateAction(UpdateActionKind.ReleaseNotes, v110.url), ios.state.value.update)

        val store = page(target = UpdateTarget(releaseHasDownload = false, storeUrl = "https://apps.apple.com/app/id1"))
        advanceUntilIdle()
        assertEquals(UpdateAction(UpdateActionKind.Store, "https://apps.apple.com/app/id1"), store.state.value.update)
    }

    @Test
    fun `on the newest version there is nothing to update to`() = runTest(main.testDispatcher) {
        repository.published = listOf(v110, v100, v090)

        val vm = page(installed = "1.1.0")
        advanceUntilIdle()
        assertNull(vm.state.value.update)
        assertTrue(vm.state.value.days.none { it.updateAvailable })
        assertEquals(listOf(true, false, false), vm.state.value.days.map { it.installed })

        // Nothing is opened: Back is the first effect.
        vm.onEvent(WhatsNewEvent.Update)
        vm.onEvent(WhatsNewEvent.Back)
        assertEquals(WhatsNewEffect.NavigateBack, vm.effect.first())
    }

    @Test
    fun `the button is there without a dot before any release is known`() = runTest(main.testDispatcher) {
        repository.failing = true
        val vm = button()
        advanceUntilIdle()

        assertEquals(WhatsNewButtonState(visible = true, unseen = false), vm.state.value)
        assertEquals(1, repository.dueChecks)
    }

    @Test
    fun `the button opens the page`() = runTest(main.testDispatcher) {
        val vm = button()
        advanceUntilIdle()

        vm.onEvent(WhatsNewButtonEvent.Open)
        assertEquals(WhatsNewButtonEffect.OpenPage, vm.effect.first())
    }

    @Test
    fun `a hidden button comes back by itself with a newer release`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100, v090)
        val vm = button()
        advanceUntilIdle()
        assertTrue(vm.state.value.visible)

        vm.onEvent(WhatsNewButtonEvent.Hide)
        advanceUntilIdle()
        assertFalse(vm.state.value.visible)
        assertEquals("1.0.0", repository.state.value.hiddenVersion)
        // Still hidden at the next start, with the same releases.
        val restarted = button()
        advanceUntilIdle()
        assertFalse(restarted.state.value.visible)

        // A newer release is published: the next daily look brings the button back, with its dot.
        repository.published = listOf(v110, v100, v090)
        val later = button()
        advanceUntilIdle()
        assertEquals(WhatsNewButtonState(visible = true, unseen = true), later.state.value)
        assertEquals(WhatsNewButtonState(visible = true, unseen = true), vm.state.value)
    }

    @Test
    fun `hidden before any release is known it waits for one newer than the installed version`() = runTest(main.testDispatcher) {
        repository.failing = true
        val vm = button(installed = "1.0.0")
        advanceUntilIdle()

        vm.onEvent(WhatsNewButtonEvent.Hide)
        advanceUntilIdle()
        assertFalse(vm.state.value.visible)
        assertEquals("1.0.0", repository.state.value.hiddenVersion)

        repository.failing = false
        repository.published = listOf(v100, v090)
        val sameVersion = button(installed = "1.0.0")
        advanceUntilIdle()
        assertFalse(sameVersion.state.value.visible)

        repository.published = listOf(v110, v100, v090)
        val newer = button(installed = "1.0.0")
        advanceUntilIdle()
        assertTrue(newer.state.value.visible)
    }

    @Test
    fun `the page stays reachable and clears the dot while the button is hidden`() = runTest(main.testDispatcher) {
        repository.published = listOf(v100, v090)
        val vm = button()
        advanceUntilIdle()
        vm.onEvent(WhatsNewButtonEvent.Hide)
        advanceUntilIdle()

        val page = page()
        advanceUntilIdle()

        assertEquals(2, page.state.value.days.size)
        assertFalse(vm.state.value.visible)
        assertFalse(vm.state.value.unseen)
    }

    @Test
    fun `with the update notice off the button never asks github`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(updateNotice = UpdateNoticeStyle.Off)
        repository.state.value = WhatsNewHistory(releases = listOf(v100, v090), seenVersion = "0.9.0")

        val vm = button()
        advanceUntilIdle()
        assertEquals(0, repository.dueChecks)
        // What is already known still shows.
        assertEquals(WhatsNewButtonState(visible = true, unseen = true), vm.state.value)

        settings.settings.value = AppSettings(updateNotice = UpdateNoticeStyle.Banner)
        advanceUntilIdle()
        assertEquals(1, repository.dueChecks)
    }

    private companion object {
        const val ISSUE = "https://github.com/beppe2880/opencode-mobile/issues/2"
    }
}

/** Keeps what it is told in memory; [published] is what GitHub answers with unless [failing]. */
private class FakeWhatsNew : WhatsNewRepository {
    val state = MutableStateFlow(WhatsNewHistory())
    override val history: StateFlow<WhatsNewHistory> = state

    var published: List<AppRelease> = emptyList()
    var failing = false
    var refreshes = 0
    var dueChecks = 0

    /** When set, [refresh] waits for it. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun refresh(): Result<List<AppRelease>> {
        refreshes++
        gate?.await()
        if (failing) return Result.failure(IOException("offline"))
        state.update { it.copy(releases = published) }
        return Result.success(published)
    }

    override suspend fun refreshIfDue() {
        dueChecks++
        if (!failing) state.update { it.copy(releases = published) }
    }

    override suspend fun markSeen() {
        val latest = state.value.latestVersion ?: return
        state.update { it.copy(seenVersion = latest) }
    }

    override suspend fun hide(installedVersion: String) {
        state.update { it.copy(hiddenVersion = it.latestVersion ?: installedVersion) }
    }
}
