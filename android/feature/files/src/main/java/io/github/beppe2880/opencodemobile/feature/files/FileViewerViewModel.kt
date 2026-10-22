package io.github.beppe2880.opencodemobile.feature.files

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.FileRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Read-only view of one file of the server. */
class FileViewerViewModel(
    target: FileTarget,
    private val files: FileRepository,
) : BaseViewModel<FileViewerState, FileViewerEvent, FileViewerEffect>(FileViewerState(target.directory, target.path)) {

    private var loadJob: Job? = null

    init {
        load()
    }

    override fun onEvent(event: FileViewerEvent) {
        when (event) {
            FileViewerEvent.Reload -> load()
            FileViewerEvent.ToggleWrap -> updateState { it.copy(wrap = !it.wrap) }
            FileViewerEvent.Back -> sendEffect(FileViewerEffect.NavigateBack)
        }
    }

    private fun load() {
        loadJob?.cancel()
        // A reload keeps the file on screen until the new copy arrives.
        updateState { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            val state = currentState
            files.read(state.directory, state.path).fold(
                onSuccess = { content -> updateState { it.copy(loading = false, content = content, error = null) } },
                onFailure = { error ->
                    updateState { it.copy(loading = false, error = error.toUiText().takeIf { _ -> it.content == null }) }
                    if (currentState.content != null) sendEffect(FileViewerEffect.ShowMessage(error.toUiText()))
                },
            )
        }
    }
}

/** The file to show: [path] is absolute or relative to [directory]. */
data class FileTarget(val directory: String, val path: String)
