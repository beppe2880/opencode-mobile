package io.github.beppe2880.opencodemobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

internal actual fun lockDialogProperties() = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
    usePlatformInsets = false,
)

/** The status bar belongs to the app's only window: it already follows the theme. */
@Composable
internal actual fun LockWindowBars(dark: Boolean) = Unit
