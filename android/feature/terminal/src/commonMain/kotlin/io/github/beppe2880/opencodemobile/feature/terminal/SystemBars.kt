package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.runtime.Composable

/** Hides the status and navigation bars while [hidden] and this is in the composition. */
@Composable
internal expect fun SystemBarsHidden(hidden: Boolean)
