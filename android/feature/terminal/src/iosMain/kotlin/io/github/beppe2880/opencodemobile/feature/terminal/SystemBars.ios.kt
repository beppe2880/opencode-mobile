package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.runtime.Composable

/** The status bar belongs to the hosting view controller; full screen hides only the title bar. */
@Composable
internal actual fun SystemBarsHidden(hidden: Boolean) = Unit
