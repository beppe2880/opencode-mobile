package io.github.beppe2880.opencodemobile.feature.terminal

import android.os.Handler
import android.os.Looper
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
import org.connectbot.terminal.ModifierManager
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import org.connectbot.terminal.VTermKey

class TerminalViewModel(
    directory: String,
    private val terminals: TerminalRepository,
    private val settings: SettingsRepository,
) : BaseViewModel<TerminalState, TerminalEvent, TerminalEffect>(TerminalState(directory)) {

    private val input = Channel<ByteArray>(Channel.UNLIMITED)
    private val size = MutableStateFlow<Pair<Int, Int>?>(null)
    private val main = Handler(Looper.getMainLooper())

    // Replaying old output makes the emulator answer queries the shell asked long ago; those
    // answers are dropped until the replay has been drawn.
    private var forwardInput = false

    /** The screen the shell draws on; kept here so it survives configuration changes. */
    val emulator: TerminalEmulator = TerminalEmulatorFactory.create(
        onKeyboardInput = { bytes -> if (forwardInput) input.trySend(bytes) },
        onResize = { size.value = it.columns to it.rows },
    )

    /** Modifiers of the key bar, also applied to the next key typed on the keyboard. */
    val modifiers: ModifierManager = object : ModifierManager {
        override fun isCtrlActive() = currentState.modifier(TermModifier.Ctrl) != ModifierState.Off
        override fun isAltActive() = currentState.modifier(TermModifier.Alt) != ModifierState.Off
        override fun isShiftActive() = currentState.modifier(TermModifier.Shift) != ModifierState.Off
        override fun clearTransients() = updateState { state ->
            state.copy(modifiers = state.modifiers.filterValues { it == ModifierState.Locked })
        }
    }

    private var session: Job? = null
    private var terminalId: String? = null

    init {
        open()
        viewModelScope.launch {
            settings.settings.map { BarKeys.fromSettings(it.terminalKeys) }.distinctUntilChanged().collect { rows ->
                updateState { it.copy(keyRows = rows) }
            }
        }
    }

    override fun onEvent(event: TerminalEvent) {
        when (event) {
            is TerminalEvent.Press -> press(event.key)
            is TerminalEvent.Paste -> if (event.text.isNotEmpty()) emulator.pasteText(event.text)
            is TerminalEvent.EditKeys -> updateState { it.copy(editingKeys = event.open) }
            is TerminalEvent.SaveKeys -> saveKeys(BarKeys.toSettings(event.rows))
            TerminalEvent.ResetKeys -> saveKeys(null)
            TerminalEvent.Retry -> open()
            TerminalEvent.Restart -> {
                // Full reset (RIS): the new shell starts on a clean screen.
                emulator.writeInput("\u001bc".toByteArray())
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
        forwardInput = false
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
                        is TerminalUpdate.Output -> emulator.writeInput(update.text.toByteArray())
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
        if (link == TerminalLink.Connected) {
            // Queued behind the emulator callbacks of the replay just written.
            main.post { forwardInput = true }
        } else {
            forwardInput = false
        }
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
                emulator.dispatchKey(modifierMask(), key.key.code)
                modifiers.clearTransients()
            }
            is BarKey.Text -> {
                val codePoints = key.text.codePoints().toArray()
                if (codePoints.size == 1) {
                    emulator.dispatchCharacter(modifierMask(), codePoints[0])
                    modifiers.clearTransients()
                } else if (forwardInput) {
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

    private fun modifierMask(): Int = currentState.modifiers
        .filterValues { it != ModifierState.Off }
        .keys.fold(0) { mask, modifier -> mask or modifier.mask }

    override fun onCleared() {
        main.removeCallbacksAndMessages(null)
        emulator.close()
    }

    private companion object {
        const val RESIZE_DEBOUNCE_MILLIS = 150L
    }
}

private val SpecialKey.code: Int
    get() = when (this) {
        SpecialKey.Escape -> VTermKey.ESCAPE
        SpecialKey.Tab -> VTermKey.TAB
        SpecialKey.Enter -> VTermKey.ENTER
        SpecialKey.Backspace -> VTermKey.BACKSPACE
        SpecialKey.Left -> VTermKey.LEFT
        SpecialKey.Down -> VTermKey.DOWN
        SpecialKey.Up -> VTermKey.UP
        SpecialKey.Right -> VTermKey.RIGHT
        SpecialKey.Home -> VTermKey.HOME
        SpecialKey.End -> VTermKey.END
        SpecialKey.PageUp -> VTermKey.PAGEUP
        SpecialKey.PageDown -> VTermKey.PAGEDOWN
        SpecialKey.Insert -> VTermKey.INS
        SpecialKey.Delete -> VTermKey.DEL
        SpecialKey.F1 -> VTermKey.FUNCTION_1
        SpecialKey.F2 -> VTermKey.FUNCTION_2
        SpecialKey.F3 -> VTermKey.FUNCTION_3
        SpecialKey.F4 -> VTermKey.FUNCTION_4
        SpecialKey.F5 -> VTermKey.FUNCTION_5
        SpecialKey.F6 -> VTermKey.FUNCTION_6
        SpecialKey.F7 -> VTermKey.FUNCTION_7
        SpecialKey.F8 -> VTermKey.FUNCTION_8
        SpecialKey.F9 -> VTermKey.FUNCTION_9
        SpecialKey.F10 -> VTermKey.FUNCTION_10
        SpecialKey.F11 -> VTermKey.FUNCTION_11
        SpecialKey.F12 -> VTermKey.FUNCTION_12
    }
