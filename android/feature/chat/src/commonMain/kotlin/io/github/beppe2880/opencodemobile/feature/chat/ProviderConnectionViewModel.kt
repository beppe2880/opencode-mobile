package io.github.beppe2880.opencodemobile.feature.chat

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.IntegrationRepository
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
}

sealed interface ProviderConnectionEffect {
    data object Connected : ProviderConnectionEffect
    data object Close : ProviderConnectionEffect
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
            ProviderConnectionEvent.Retry -> load()
            is ProviderConnectionEvent.Search -> updateState { it.copy(query = event.query) }
            is ProviderConnectionEvent.Select -> if (!currentState.saving) {
                val integration = currentState.integrations.firstOrNull { it.id == event.id } ?: return
                updateState { it.copy(selected = integration, answers = integration.defaults(), key = "", failed = false, connected = false) }
            }
            is ProviderConnectionEvent.KeyChanged -> if (!currentState.saving) updateState { it.copy(key = event.key, failed = false) }
            is ProviderConnectionEvent.Answer -> if (!currentState.saving) updateState {
                it.copy(answers = if (event.value == null) it.answers - event.key else it.answers + (event.key to event.value), failed = false)
            }
            ProviderConnectionEvent.Back -> if (!currentState.saving) updateState {
                it.copy(selected = null, key = "", answers = emptyMap(), failed = false, connected = false)
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
        if (state.saving || state.connected || state.key.isBlank() || !selected.isComplete(state.answers)) return
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
