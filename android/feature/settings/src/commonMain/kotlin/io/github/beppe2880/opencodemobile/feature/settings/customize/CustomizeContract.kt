package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent

@Immutable
data class CustomizeState(
    val settings: AppSettings = AppSettings(),
    /** The part of the chat whose options are open; null while none is. */
    val selected: ChatElement? = null,
    /** Outlines and edit badges mark the parts that can be tapped. */
    val marks: Boolean = true,
) {
    /** The options of [selected] that apply with the current settings. */
    internal val options: List<ChatOption> get() = selected?.optionsFor(settings).orEmpty()
}

sealed interface CustomizeEvent {
    data object Back : CustomizeEvent

    /** A part of the preview was tapped: its options open. */
    data class Select(val element: ChatElement) : CustomizeEvent
    data object CloseOptions : CustomizeEvent
    data class MarksChanged(val visible: Boolean) : CustomizeEvent

    /** A switch or choice of the open options, the same event the settings list sends. */
    data class Setting(val event: SettingsEvent) : CustomizeEvent
}

sealed interface CustomizeEffect {
    data object NavigateBack : CustomizeEffect
}

/**
 * The chat drawn with the given settings, supplied by the app, which knows the chat: tapping a
 * part calls the callback; marks outline the tappable parts and the selected one is highlighted.
 */
typealias ChatPreviewContent = @Composable (
    settings: AppSettings,
    marks: Boolean,
    selected: ChatElement?,
    onCustomize: (ChatElement) -> Unit,
    modifier: Modifier,
) -> Unit
