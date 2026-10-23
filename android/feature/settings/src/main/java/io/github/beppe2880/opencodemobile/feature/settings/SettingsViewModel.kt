package io.github.beppe2880.opencodemobile.feature.settings

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val servers: ServerRepository,
    private val settings: SettingsRepository,
    appInfo: AppInfo,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(
    SettingsState(appVersion = appInfo.versionName, apiVersion = appInfo.apiVersion),
) {
    private var probeJob: Job? = null

    /** The switch asked for the notification permission and turns on once it is granted. */
    private var awaitingPermission = false

    init {
        viewModelScope.launch { servers.servers.collect { list -> updateState { it.copy(servers = list) } } }
        viewModelScope.launch { settings.settings.collect { s -> updateState { it.copy(settings = s) } } }
        viewModelScope.launch {
            servers.activeServer.distinctUntilChangedBy { it?.id }.collect { active ->
                updateState { it.copy(activeServerId = active?.id) }
                probe(active)
            }
        }
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Back -> sendEffect(SettingsEffect.NavigateBack)
            SettingsEvent.AddServer -> sendEffect(SettingsEffect.OpenConnect)
            SettingsEvent.OpenUsage -> sendEffect(SettingsEffect.OpenUsage)
            is SettingsEvent.SelectServer -> viewModelScope.launch { servers.activate(event.serverId) }
            is SettingsEvent.RequestRename -> updateState { it.copy(renaming = event.server) }
            is SettingsEvent.RequestRemove -> updateState { it.copy(removing = event.server) }
            SettingsEvent.DismissDialog -> updateState { it.copy(renaming = null, removing = null) }
            is SettingsEvent.ConfirmRename -> {
                val server = currentState.renaming ?: return
                updateState { it.copy(renaming = null) }
                if (event.name.isNotBlank() && event.name != server.name) {
                    viewModelScope.launch { servers.rename(server.id, event.name.trim()) }
                }
            }
            SettingsEvent.ConfirmRemove -> {
                val server = currentState.removing ?: return
                updateState { it.copy(removing = null) }
                viewModelScope.launch { servers.remove(server.id) }
            }
            is SettingsEvent.ThemeChanged -> update { it.copy(theme = event.theme) }
            is SettingsEvent.ShowReasoningChanged -> update { it.copy(showReasoning = event.enabled) }
            is SettingsEvent.ExpandToolsChanged -> update { it.copy(expandToolsByDefault = event.enabled) }
            is SettingsEvent.FollowRunningToolsChanged -> update { it.copy(followRunningTools = event.enabled) }
            is SettingsEvent.LiveThinkingChanged -> update { it.copy(liveThinking = event.enabled) }
            is SettingsEvent.LiveShellChanged -> update { it.copy(liveShell = event.enabled) }
            is SettingsEvent.LiveEditsChanged -> update { it.copy(liveEdits = event.enabled) }
            is SettingsEvent.LiveReadsChanged -> update { it.copy(liveReads = event.enabled) }
            is SettingsEvent.LiveOtherChanged -> update { it.copy(liveOther = event.enabled) }
            is SettingsEvent.ShowFullCommandsChanged -> update { it.copy(showFullCommands = event.enabled) }
            is SettingsEvent.ShowToolDetailsChanged -> update { it.copy(showToolDetails = event.enabled) }
            is SettingsEvent.ShowCostChanged -> update { it.copy(showCost = event.enabled) }
            is SettingsEvent.SwipeBetweenChatsChanged -> update { it.copy(swipeBetweenChats = event.enabled) }
            is SettingsEvent.AgentColorOutlineChanged -> update { it.copy(agentColorOutline = event.enabled) }
            is SettingsEvent.DictationChanged -> update { it.copy(dictation = event.enabled) }
            is SettingsEvent.GroupActivityChanged -> update { it.copy(groupActivity = event.enabled) }
            is SettingsEvent.NotificationsChanged -> when {
                !event.enabled || currentState.notificationsAllowed -> update { it.copy(notifications = event.enabled) }
                else -> {
                    awaitingPermission = true
                    sendEffect(
                        if (currentState.notificationsBlocked) SettingsEffect.OpenNotificationSettings
                        else SettingsEffect.RequestNotificationPermission,
                    )
                }
            }
            is SettingsEvent.NotifyPermissionsChanged -> update { it.copy(notifyPermissions = event.enabled) }
            is SettingsEvent.NotifyQuestionsChanged -> update { it.copy(notifyQuestions = event.enabled) }
            is SettingsEvent.NotifyFinishedChanged -> update { it.copy(notifyFinished = event.enabled) }
            is SettingsEvent.NotificationAccess -> {
                updateState {
                    it.copy(
                        notificationsAllowed = event.allowed,
                        notificationsBlocked = !event.allowed && (event.blocked || it.notificationsBlocked),
                    )
                }
                // Granting the permission asked for by the switch turns notifications on.
                if (event.allowed && awaitingPermission) update { it.copy(notifications = true) }
                if (event.allowed || event.blocked) awaitingPermission = false
            }
        }
    }

    private fun update(transform: (io.github.beppe2880.opencodemobile.core.domain.AppSettings) -> io.github.beppe2880.opencodemobile.core.domain.AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private fun probe(server: ServerProfile?) {
        probeJob?.cancel()
        if (server == null) {
            updateState { it.copy(serverVersion = null, serverVersionLoading = false) }
            return
        }
        updateState { it.copy(serverVersionLoading = true) }
        probeJob = viewModelScope.launch {
            val version = servers.probe(server.baseUrl, server.secret).getOrNull()?.version
            updateState { it.copy(serverVersion = version, serverVersionLoading = false) }
        }
    }
}
