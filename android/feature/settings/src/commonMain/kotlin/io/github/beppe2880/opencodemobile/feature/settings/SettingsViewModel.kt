package io.github.beppe2880.opencodemobile.feature.settings

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.SpeechToTextRepository
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.github.beppe2880.opencodemobile.core.domain.reset
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val servers: ServerRepository,
    private val settings: SettingsRepository,
    appInfo: AppInfo,
    private val stt: SpeechToTextRepository,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(
    SettingsState(appVersion = appInfo.versionName, apiVersion = appInfo.apiVersion, updateTarget = appInfo.updateTarget),
) {
    private var probeJob: Job? = null
    private var sttJob: Job? = null

    /** The switch asked for the notification permission and turns on once it is granted. */
    private var awaitingPermission = false

    init {
        viewModelScope.launch { servers.servers.collect { list -> updateState { it.copy(servers = list) } } }
        viewModelScope.launch { settings.settings.collect { s -> updateState { it.copy(settings = s) } } }
        viewModelScope.launch { stt.savedKeys.collect { keys -> updateState { it.copy(sttKeys = keys) } } }
        viewModelScope.launch {
            servers.activeServer.distinctUntilChangedBy { it?.id }.collect { active ->
                updateState { it.copy(activeServerId = active?.id) }
                probe(active)
            }
        }
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Back -> if (currentState.area != null) closeArea() else sendEffect(SettingsEffect.NavigateBack)
            SettingsEvent.AddServer -> sendEffect(SettingsEffect.OpenConnect)
            SettingsEvent.OpenUsage -> sendEffect(SettingsEffect.OpenUsage)
            is SettingsEvent.OpenArea -> updateState {
                val part = event.part ?: event.option?.part ?: SettingsPart.of(event.area).first()
                it.copy(area = event.area, selected = part, focus = event.option)
            }
            SettingsEvent.CloseArea -> closeArea()
            // A part of another group (the composer in the chat) opens that group.
            is SettingsEvent.SelectPart -> updateState { it.copy(area = event.part.area, selected = event.part, focus = null) }
            is SettingsEvent.PreviewRunning -> updateState { it.copy(running = event.running) }
            is SettingsEvent.QueryChanged -> updateState { it.copy(query = event.query) }
            is SettingsEvent.SelectServer -> viewModelScope.launch { servers.activate(event.serverId) }
            is SettingsEvent.RequestRename -> updateState { it.copy(renaming = event.server) }
            is SettingsEvent.RequestRemove -> updateState { it.copy(removing = event.server) }
            SettingsEvent.DismissDialog -> updateState { it.copy(renaming = null, removing = null, resetting = false) }
            SettingsEvent.RequestReset -> updateState { it.copy(resetting = true) }
            SettingsEvent.ConfirmReset -> {
                updateState { it.copy(resetting = false) }
                update { it.reset() }
            }
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
            is SettingsEvent.SaveSttKey -> {
                val provider = settings.settings.value.sttProvider
                clearSttTest()
                viewModelScope.launch { stt.saveKey(provider, event.key) }
            }
            SettingsEvent.RemoveSttKey -> {
                val provider = settings.settings.value.sttProvider
                clearSttTest()
                viewModelScope.launch { stt.removeKey(provider) }
            }
            SettingsEvent.TestStt -> {
                sttJob?.cancel()
                updateState { it.copy(sttTest = SttTest.Running) }
                sttJob = viewModelScope.launch {
                    val result = stt.test()
                    updateState {
                        it.copy(
                            sttTest = result.exceptionOrNull()?.let { e ->
                                SttTest.Failed(e as? TranscriptionError ?: TranscriptionError.Network(e))
                            } ?: SttTest.Passed,
                        )
                    }
                }
            }
            SettingsEvent.LoadSttModels -> {
                val provider = settings.settings.value.sttProvider
                if (currentState.sttModelsLoading) return
                updateState { it.copy(sttModelsLoading = true) }
                viewModelScope.launch {
                    val models = stt.listModels(provider).getOrNull()
                    updateState { state ->
                        state.copy(
                            sttModelsLoading = false,
                            sttListedModels = if (models.isNullOrEmpty()) state.sttListedModels else state.sttListedModels + (provider to models),
                        )
                    }
                }
            }
            // Switches and choices of a single setting.
            else -> {
                // A test speaks for the provider, address and model it was made with.
                if (event is SettingsEvent.SttProviderChanged || event is SettingsEvent.SttModelChanged || event is SettingsEvent.SttBaseUrlChanged) {
                    clearSttTest()
                }
                update { it.updatedBy(event) ?: it }
            }
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private fun clearSttTest() {
        sttJob?.cancel()
        updateState { it.copy(sttTest = null) }
    }

    private fun closeArea() = updateState { it.copy(area = null, selected = null, focus = null) }

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
