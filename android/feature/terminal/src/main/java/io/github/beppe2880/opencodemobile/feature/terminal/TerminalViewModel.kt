package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalUpdate
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TerminalViewModel(
    directory: String,
    private val terminals: TerminalRepository,
    private val settings: SettingsRepository,
) : BaseViewModel<TerminalState, TerminalEvent, TerminalEffect>(TerminalState(directory)) {

    private val input = Channel<ByteArray>(Channel.UNLIMITED)
    private val size = MutableStateFlow<Pair<Int, Int>?>(null)

    /** The screen the shell draws on; kept here so it survives configuration changes. */
    val surface = WebSurface(send = { input.trySend(it) }, onResize = ::onResize, onModifiersUsed = ::clearTransients)

    private var session: Job? = null
    private var terminalId: String? = null

    init {
        open()
        viewModelScope.launch {
            settings.settings.map { BarKeys.fromSettings(it.terminalKeys) }.distinctUntilChanged().collect { rows ->
                updateState { it.copy(keyRows = rows) }
            }
        }
        viewModelScope.launch {
            state.map { modifierMask(it) }.distinctUntilChanged().collect(surface::setModifiers)
        }
    }

    private fun onResize(columns: Int, rows: Int) {
        size.value = columns to rows
    }

    private fun clearTransients() = updateState { state ->
        state.copy(modifiers = state.modifiers.filterValues { it == ModifierState.Locked })
    }

    override fun onEvent(event: TerminalEvent) {
        when (event) {
            is TerminalEvent.Press -> press(event.key)
            is TerminalEvent.Paste -> if (event.text.isNotEmpty()) surface.paste(event.text)
            is TerminalEvent.EditKeys -> updateState { it.copy(editingKeys = event.open) }
            is TerminalEvent.SaveKeys -> saveKeys(BarKeys.toSettings(event.rows))
            TerminalEvent.ResetKeys -> saveKeys(null)
            TerminalEvent.Retry -> open()
            TerminalEvent.Restart -> {
                surface.reset()
                open()
            }
            TerminalEvent.AskClose -> updateState { it.copy(confirmClose = true) }
            TerminalEvent.DismissClose -> updateState { it.copy(confirmClose = false) }
            TerminalEvent.ConfirmClose -> closeTerminal()
            TerminalEvent.Back -> sendEffect(TerminalEffect.NavigateBack)
        }
    }

    @OptIn(FlowPreview::class)
    private fun open() {
        session?.cancel()
        surface.setLive(false)
        updateState { it.copy(link = TerminalLink.Connecting, error = null) }
        session = viewModelScope.launch {
            val directory = currentState.directory
            val terminal = terminals.open(directory).getOrElse { error ->
                updateState { it.copy(error = error.toUiText()) }
                return@launch
            }
            terminalId = terminal.id
            updateState { it.copy(title = terminal.title) }
            val resizing = launch {
                size.filterNotNull().debounce(RESIZE_DEBOUNCE_MILLIS).collectLatest { (columns, rows) ->
                    terminals.resize(directory, terminal.id, columns, rows)
                }
            }
            try {
                terminals.attach(directory, terminal.id, input.receiveAsFlow()).collect { update ->
                    when (update) {
                        is TerminalUpdate.Output -> surface.write(update.text)
                        is TerminalUpdate.Link -> onLink(update.state)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState { it.copy(error = e.toUiText()) }
            } finally {
                resizing.cancel()
            }
        }
    }

    private fun onLink(link: TerminalLink) {
        surface.setLive(link == TerminalLink.Connected)
        updateState { it.copy(link = link) }
    }

    private fun closeTerminal() {
        updateState { it.copy(confirmClose = false) }
        // Exited or never opened: only leave.
        val id = terminalId.takeIf { currentState.link != TerminalLink.Exited }
        viewModelScope.launch {
            if (id != null) {
                terminals.close(currentState.directory, id).onFailure { error ->
                    sendEffect(TerminalEffect.ShowMessage(error.toUiText()))
                    return@launch
                }
            }
            session?.cancel()
            sendEffect(TerminalEffect.NavigateBack)
        }
    }

    private fun press(key: BarKey) {
        when (key) {
            is BarKey.Modifier -> updateState { state ->
                state.copy(modifiers = state.modifiers + (key.modifier to state.modifier(key.modifier).next()))
            }
            is BarKey.Special -> {
                surface.key(key.key, modifierMask(currentState))
                clearTransients()
            }
            is BarKey.Text -> {
                val codePoints = key.text.codePoints().toArray()
                if (codePoints.size == 1) {
                    surface.character(codePoints[0], modifierMask(currentState))
                    clearTransients()
                } else if (currentState.link == TerminalLink.Connected) {
                    // Macros go to the shell as they are, not as a bracketed paste.
                    input.trySend(key.text.toByteArray())
                }
            }
            // Read from the clipboard by the screen and delivered as TerminalEvent.Paste.
            BarKey.Paste -> Unit
        }
    }

    private fun saveKeys(rows: List<List<String>>?) {
        viewModelScope.launch { settings.update { it.copy(terminalKeys = rows) } }
    }

    private fun modifierMask(state: TerminalState): Int = state.modifiers
        .filterValues { it != ModifierState.Off }
        .keys.fold(0) { mask, modifier -> mask or modifier.mask }

    override fun onCleared() = surface.close()

    private companion object {
        const val RESIZE_DEBOUNCE_MILLIS = 150L
    }
}
