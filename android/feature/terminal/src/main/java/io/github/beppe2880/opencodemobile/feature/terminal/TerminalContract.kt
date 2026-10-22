package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

@Immutable
data class TerminalState(
    val directory: String,
    val title: String = "",
    val link: TerminalLink = TerminalLink.Connecting,
    /** Opening the terminal failed; nothing is attached. */
    val error: UiText? = null,
    /** Rows of the key bar above the keyboard, as the user arranged them. */
    val keyRows: List<List<BarKey>> = BarKeys.Default,
    /** Modifiers of the key bar, also applied to keys typed on the keyboard. */
    val modifiers: Map<TermModifier, ModifierState> = emptyMap(),
    val editingKeys: Boolean = false,
    val confirmClose: Boolean = false,
) {
    fun modifier(modifier: TermModifier): ModifierState = modifiers[modifier] ?: ModifierState.Off
}

sealed interface TerminalEvent {
    /** A key of the bar; [BarKey.Paste] arrives as [Paste] with the clipboard text. */
    data class Press(val key: BarKey) : TerminalEvent
    data class Paste(val text: String) : TerminalEvent
    data class EditKeys(val open: Boolean) : TerminalEvent
    data class SaveKeys(val rows: List<List<BarKey>>) : TerminalEvent
    data object ResetKeys : TerminalEvent
    data object Retry : TerminalEvent

    /** After the shell exited: start a new one in the same folder. */
    data object Restart : TerminalEvent
    data object AskClose : TerminalEvent
    data object DismissClose : TerminalEvent
    data object ConfirmClose : TerminalEvent
    data object Back : TerminalEvent
}

sealed interface TerminalEffect {
    data class ShowMessage(val message: UiText) : TerminalEffect
    data object NavigateBack : TerminalEffect
}
