package io.github.beppe2880.opencodemobile.feature.chat

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.IntegrationRepository
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthAttempt
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthMethod
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStatus
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProviderConnectionState(
    val serverName: String = "",
    val loading: Boolean = false,
    val integrations: List<KeyIntegration> = emptyList(),
    val selected: KeyIntegration? = null,
    val answers: Map<String, FormAnswer> = emptyMap(),
    val key: String = "",
    val saving: Boolean = false,
    val failed: Boolean = false,
    val connected: Boolean = false,
    val query: String = "",
    val oauth: ProviderOAuthMethod? = null,
    val attempt: ProviderOAuthAttempt? = null,
    val visible: Boolean = true,
    val browserUrl: String? = null,
)

sealed interface ProviderConnectionEvent {
    data class Open(val directory: String) : ProviderConnectionEvent
    data object Close : ProviderConnectionEvent
    data object Retry : ProviderConnectionEvent
    data class Select(val id: String) : ProviderConnectionEvent
    data class KeyChanged(val key: String) : ProviderConnectionEvent
    data class Answer(val key: String, val value: FormAnswer?) : ProviderConnectionEvent
    data class Search(val query: String) : ProviderConnectionEvent
    data object Back : ProviderConnectionEvent
    data object Submit : ProviderConnectionEvent
    data class ChooseOAuth(val id: String) : ProviderConnectionEvent
    data object StartOAuth : ProviderConnectionEvent
    data object OpenBrowser : ProviderConnectionEvent
    data class Visible(val value: Boolean) : ProviderConnectionEvent
}

sealed interface ProviderConnectionEffect {
    data object Connected : ProviderConnectionEffect
    data object Close : ProviderConnectionEffect
    data class OpenBrowser(val url: String) : ProviderConnectionEffect
}

class ProviderConnectionViewModel(
    private val integrations: IntegrationRepository,
    private val servers: ServerRepository,
    private val catalog: CatalogRepository,
) : BaseViewModel<ProviderConnectionState, ProviderConnectionEvent, ProviderConnectionEffect>(ProviderConnectionState()) {
    private var serverId: String? = null
    private var directory = ""
    private var job: Job? = null
    private var generation = 0

    init {
        viewModelScope.launch {
            servers.activeServer.collect { server ->
                if (serverId != null && server?.id != serverId) {
                    close()
                    sendEffect(ProviderConnectionEffect.Close)
                }
            }
        }
    }

    override fun onEvent(event: ProviderConnectionEvent) {
        when (event) {
            is ProviderConnectionEvent.Open -> {
                close()
                val server = servers.activeServer.value ?: return
                serverId = server.id
                directory = event.directory
                updateState { it.copy(serverName = server.name) }
                load()
            }
            ProviderConnectionEvent.Close -> close()
            ProviderConnectionEvent.Retry -> if (currentState.attempt != null) poll() else load()
            is ProviderConnectionEvent.Visible -> {
                updateState { it.copy(visible = event.value, failed = if (event.value && it.attempt != null) false else it.failed) }
            }
            ProviderConnectionEvent.OpenBrowser -> (currentState.browserUrl ?: currentState.attempt?.url)?.let {
                sendEffect(ProviderConnectionEffect.OpenBrowser(it))
            }
            ProviderConnectionEvent.StartOAuth -> startOAuth()
            is ProviderConnectionEvent.ChooseOAuth -> {
                if (currentState.saving || currentState.attempt != null) return
                val method = currentState.selected?.oauthMethods?.firstOrNull { it.id == event.id && it.mobileSupported } ?: return
                updateState { it.copy(oauth = method, answers = method.form.defaults(), key = "", failed = false) }
            }
            is ProviderConnectionEvent.Search -> updateState { it.copy(query = event.query) }
            is ProviderConnectionEvent.Select -> if (!currentState.saving) {
                val integration = currentState.integrations.firstOrNull { it.id == event.id } ?: return
                updateState {
                    it.copy(selected = integration, answers = integration.defaults(), key = "", failed = false, connected = false, oauth = null, attempt = null)
                }
            }
            is ProviderConnectionEvent.KeyChanged -> if (!currentState.saving) updateState { it.copy(key = event.key, failed = false) }
            is ProviderConnectionEvent.Answer -> if (!currentState.saving) updateState {
                it.copy(answers = if (event.value == null) it.answers - event.key else it.answers + (event.key to event.value), failed = false)
            }
            ProviderConnectionEvent.Back -> {
                close()
                onEvent(ProviderConnectionEvent.Open(directory))
            }
            ProviderConnectionEvent.Submit -> submit()
        }
    }

    private fun close() {
        generation++
        job?.cancel()
        serverId = null
        updateState { ProviderConnectionState() }
    }

    private fun startOAuth() {
        val state = currentState
        val method = state.oauth ?: return
        val selected = state.selected ?: return
        val id = serverId ?: return
        val folder = directory
        if (state.saving || state.attempt != null || !method.mobileSupported || !method.form.isComplete(state.answers)) return
        if (method.browser) {
            startBrowserOAuth(id, folder, selected.id, method.id)
            return
        }
        val token = generation
        updateState { it.copy(saving = true, failed = false) }
        job = viewModelScope.launch {
            var pending: ProviderOAuthAttempt? = null
            try {
                val result = integrations.startOAuth(id, folder, selected.id, method.id, method.form.answers(state.answers))
                pending = result.getOrNull()
                if (token != generation) return@launch
                result.fold(
                    onSuccess = { attempt ->
                        updateState { it.copy(attempt = attempt, saving = false, answers = emptyMap()) }
                        while (true) {
                            this@ProviderConnectionViewModel.state.first { it.visible && !it.failed }
                            val status = integrations.oauthStatus(id, folder, selected.id, attempt.id)
                            if (token != generation) return@launch
                            if (status.isFailure) {
                                updateState { it.copy(failed = true) }
                                // Pause network retries until the user requests another check.
                                continue
                            }
                            when (status.getOrThrow()) {
                                ProviderOAuthStatus.Pending -> delay(1_000)
                                ProviderOAuthStatus.Complete -> {
                                    pending = null
                                    catalog.invalidateModels(id)
                                    updateState { it.copy(attempt = null, connected = true) }
                                    sendEffect(ProviderConnectionEffect.Connected)
                                    return@launch
                                }
                                ProviderOAuthStatus.Failed, ProviderOAuthStatus.Expired -> {
                                    pending = null
                                    updateState { it.copy(attempt = null, failed = true) }
                                    return@launch
                                }
                            }
                        }
                    },
                    onFailure = { updateState { it.copy(saving = false, failed = true) } },
                )
            } finally {
                pending?.let { attempt ->
                    withContext(NonCancellable) { integrations.cancelOAuth(id, folder, selected.id, attempt.id) }
                }
            }
        }
    }

    private fun startBrowserOAuth(id: String, folder: String, integrationId: String, methodId: String) {
        val token = generation
        catalog.invalidateModels(id)
        updateState { it.copy(saving = true, failed = false, key = "", answers = emptyMap()) }
        job = viewModelScope.launch {
            val result = integrations.connectBrowserOAuth(id, folder, integrationId, methodId) { url ->
                withContext(Dispatchers.Main.immediate) {
                    if (token == generation) {
                        updateState { it.copy(browserUrl = url) }
                        sendEffect(ProviderConnectionEffect.OpenBrowser(url))
                    }
                }
            }
            if (result.isSuccess) catalog.invalidateModels(id)
            if (token != generation) return@launch
            updateState { it.copy(saving = false, browserUrl = null, failed = result.isFailure, connected = result.isSuccess) }
            if (result.isSuccess) sendEffect(ProviderConnectionEffect.Connected)
        }
    }

    private fun poll() {
        updateState { it.copy(failed = false) }
    }

    private fun load() {
        val id = serverId ?: return
        if (currentState.saving) return
        job?.cancel()
        val token = ++generation
        updateState { it.copy(loading = true, failed = false) }
        job = viewModelScope.launch {
            val result = integrations.listKeys(id, directory)
            if (token != generation) return@launch
            result.fold(
                onSuccess = { list -> updateState { it.copy(integrations = list, loading = false) } },
                onFailure = { updateState { it.copy(loading = false, failed = true) } },
            )
        }
    }

    private fun submit() {
        val state = currentState
        val selected = state.selected ?: return
        val id = serverId ?: return
        if (!selected.hasKey || state.oauth != null || state.saving || state.connected || state.key.isBlank() || !selected.isComplete(state.answers)) return
        val token = generation
        // Invalidate before writing too: cancellation cannot prove the server did not save.
        catalog.invalidateModels(id)
        // Never persist key or answers in SavedStateHandle, preferences, drafts or transcript.
        updateState { it.copy(saving = true, failed = false) }
        job = viewModelScope.launch {
            val result = integrations.connectKey(id, directory, selected.id, state.key, selected.answers(state.answers))
            // Even if the sheet closed, invalidate the original server after a successful write.
            if (result.isSuccess) catalog.invalidateModels(id)
            if (token != generation) return@launch
            result.fold(
                onSuccess = {
                    updateState { it.copy(saving = false, connected = true, key = "", answers = emptyMap()) }
                    sendEffect(ProviderConnectionEffect.Connected)
                },
                // Do not display raw server errors: they may echo credential material.
                onFailure = { updateState { it.copy(saving = false, failed = true, key = "") } },
            )
        }
    }
}
