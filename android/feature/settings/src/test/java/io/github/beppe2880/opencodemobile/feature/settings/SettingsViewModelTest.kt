package io.github.beppe2880.opencodemobile.feature.settings

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val servers = FakeServers()
    private val settings = FakeSettings()
    private fun vm() = SettingsViewModel(servers, settings, AppInfo("1.0", "2.0.27"))

    @Test
    fun `exposes servers, versions and settings`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(2, state.servers.size)
        assertEquals("a", state.activeServerId)
        assertEquals("2.0.27-server", state.serverVersion)
        assertEquals("1.0", state.appVersion)
        assertEquals("2.0.27", state.apiVersion)
    }

    @Test
    fun `theme and toggles are persisted`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(SettingsEvent.ThemeChanged(ThemeMode.Dark))
        vm.onEvent(SettingsEvent.ShowReasoningChanged(false))
        vm.onEvent(SettingsEvent.ExpandToolsChanged(true))
        vm.onEvent(SettingsEvent.ShowCostChanged(false))
        vm.onEvent(SettingsEvent.SwipeBetweenChatsChanged(false))
        vm.onEvent(SettingsEvent.AgentColorOutlineChanged(false))
        vm.onEvent(SettingsEvent.GroupActivityChanged(false))
        vm.onEvent(SettingsEvent.DictationChanged(false))
        vm.onEvent(SettingsEvent.FollowRunningToolsChanged(false))
        vm.onEvent(SettingsEvent.ShowFullCommandsChanged(true))
        vm.onEvent(SettingsEvent.ShowToolDetailsChanged(false))
        vm.onEvent(SettingsEvent.OutputPreviewLinesChanged(24))
        vm.onEvent(SettingsEvent.LiveOutputIntervalChanged(1_500))
        vm.onEvent(SettingsEvent.KeepLiveStepsOpenChanged(true))
        vm.onEvent(SettingsEvent.ChatTextScaleChanged(0.85f))
        vm.onEvent(SettingsEvent.LiveThinkingChanged(false))
        vm.onEvent(SettingsEvent.LiveShellChanged(false))
        vm.onEvent(SettingsEvent.LiveEditsChanged(false))
        vm.onEvent(SettingsEvent.LiveReadsChanged(true))
        vm.onEvent(SettingsEvent.LiveOtherChanged(true))
        advanceUntilIdle()

        assertEquals(
            AppSettings(
                ThemeMode.Dark,
                showReasoning = false,
                expandToolsByDefault = true,
                followRunningTools = false,
                liveThinking = false,
                liveShell = false,
                liveEdits = false,
                liveReads = true,
                liveOther = true,
                showFullCommands = true,
                showToolDetails = false,
                outputPreviewLines = 24,
                liveOutputIntervalMs = 1_500,
                keepLiveStepsOpen = true,
                chatTextScale = 0.85f,
                showCost = false,
                swipeBetweenChats = false,
                agentColorOutline = false,
                dictation = false,
                groupActivity = false,
            ),
            settings.settings.value,
        )
        assertEquals(settings.settings.value, vm.state.value.settings)
    }

    @Test
    fun `notifications turn on only once the permission is granted`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(notifications = false)
        val vm = vm()
        vm.onEvent(SettingsEvent.NotificationAccess(allowed = false))
        val effect = async { vm.effect.first() }
        vm.onEvent(SettingsEvent.NotificationsChanged(true))
        advanceUntilIdle()
        assertEquals(SettingsEffect.RequestNotificationPermission, effect.await())
        assertFalse(settings.settings.value.notifications)

        vm.onEvent(SettingsEvent.NotificationAccess(allowed = true))
        advanceUntilIdle()
        assertTrue(settings.settings.value.notifications)

        vm.onEvent(SettingsEvent.NotificationsChanged(false))
        advanceUntilIdle()
        assertFalse(settings.settings.value.notifications)
    }

    @Test
    fun `a denied permission sends the next attempt to the system settings`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(notifications = false)
        val vm = vm()
        vm.onEvent(SettingsEvent.NotificationAccess(allowed = false))
        vm.onEvent(SettingsEvent.NotificationsChanged(true))
        assertEquals(SettingsEffect.RequestNotificationPermission, vm.effect.first())
        vm.onEvent(SettingsEvent.NotificationAccess(allowed = false, blocked = true))
        advanceUntilIdle()
        assertTrue(vm.state.value.notificationsBlocked)
        assertFalse(settings.settings.value.notifications)

        vm.onEvent(SettingsEvent.NotificationsChanged(true))
        assertEquals(SettingsEffect.OpenNotificationSettings, vm.effect.first())
    }

    @Test
    fun `allowing notifications elsewhere does not turn them on`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(notifications = false)
        val vm = vm()
        vm.onEvent(SettingsEvent.NotificationAccess(allowed = false))
        vm.onEvent(SettingsEvent.NotificationAccess(allowed = true))
        advanceUntilIdle()
        assertFalse(settings.settings.value.notifications)
    }

    @Test
    fun `remove requires confirmation`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val target = vm.state.value.servers.last()
        vm.onEvent(SettingsEvent.RequestRemove(target))
        assertEquals(target, vm.state.value.removing)
        vm.onEvent(SettingsEvent.ConfirmRemove)
        advanceUntilIdle()

        assertNull(vm.state.value.removing)
        assertEquals(listOf("a"), vm.state.value.servers.map { it.id })
    }

    @Test
    fun `reset needs confirmation and keeps intros and terminal keys`() = runTest(main.testDispatcher) {
        val keys = listOf(listOf("esc"))
        settings.settings.value = AppSettings(
            theme = ThemeMode.Dark,
            outputPreviewLines = 50,
            chatTextScale = 1.3f,
            notificationsIntroShown = true,
            terminalKeys = keys,
        )
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(SettingsEvent.RequestReset)
        assertTrue(vm.state.value.resetting)
        vm.onEvent(SettingsEvent.DismissDialog)
        advanceUntilIdle()
        assertFalse(vm.state.value.resetting)
        assertEquals(ThemeMode.Dark, settings.settings.value.theme)

        vm.onEvent(SettingsEvent.RequestReset)
        vm.onEvent(SettingsEvent.ConfirmReset)
        advanceUntilIdle()

        assertFalse(vm.state.value.resetting)
        assertEquals(AppSettings(notificationsIntroShown = true, terminalKeys = keys), settings.settings.value)
    }

    @Test
    fun `rename trims and ignores blank names`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SettingsEvent.RequestRename(vm.state.value.servers.first()))
        vm.onEvent(SettingsEvent.ConfirmRename("  Laptop "))
        advanceUntilIdle()
        assertEquals("Laptop", vm.state.value.servers.first().name)
    }

    @Test
    fun `add server opens connect`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(SettingsEvent.AddServer)
        assertEquals(SettingsEffect.OpenConnect, vm.effect.first())
    }
}

private class FakeServers : ServerRepository {
    private fun p(id: String) = ServerProfile(id, "Server $id", "http://$id", secret = "x", credential = CredentialKind.Password, addedAt = 0)
    override val servers = MutableStateFlow(listOf(p("a"), p("b")))
    override val activeServer = MutableStateFlow<ServerProfile?>(p("a"))
    override suspend fun probe(baseUrl: String, password: String?) = Result.success(ServerInfo("2.0.27-server", emptyList()))
    override suspend fun pair(baseUrl: String, code: String, name: String?) = Result.success(p("c"))
    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?) = Result.success(p("c"))
    override suspend fun activate(serverId: String) {
        activeServer.value = servers.value.first { it.id == serverId }
    }
    override suspend fun rename(serverId: String, name: String) {
        servers.value = servers.value.map { if (it.id == serverId) it.copy(name = name) else it }
    }
    override suspend fun remove(serverId: String) {
        servers.value = servers.value.filterNot { it.id == serverId }
    }
}

private class FakeSettings : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
}
