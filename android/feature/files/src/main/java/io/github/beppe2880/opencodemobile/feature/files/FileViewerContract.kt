package io.github.beppe2880.opencodemobile.feature.files

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.FileContent
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

@Immutable
data class FileViewerState(
    /** Folder of the session the file was opened from. */
    val directory: String,
    /** Absolute, or relative to [directory]. */
    val path: String,
    val loading: Boolean = true,
    val content: FileContent? = null,
    /** Reading failed and nothing is shown. */
    val error: UiText? = null,
    val wrap: Boolean = true,
) {
    val name: String get() = path.trimEnd('/').substringAfterLast('/')

    /** The path as the session sees it: relative inside its folder, absolute outside. */
    val displayPath: String
        get() = directory.trimEnd('/').takeIf { it.isNotEmpty() && path.startsWith("$it/") }
            ?.let { path.removePrefix("$it/") }
            ?: path
}

sealed interface FileViewerEvent {
    data object Reload : FileViewerEvent
    data object ToggleWrap : FileViewerEvent
    data object Back : FileViewerEvent
}

sealed interface FileViewerEffect {
    data object NavigateBack : FileViewerEffect

    /** A reload failed while the earlier copy stays on screen. */
    data class ShowMessage(val message: UiText) : FileViewerEffect
}
