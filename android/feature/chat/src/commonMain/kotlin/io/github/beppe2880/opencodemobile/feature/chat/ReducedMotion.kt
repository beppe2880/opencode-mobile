package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable

/** Whether the system asks for less motion: animations off on Android, Reduce Motion on iOS. */
@Composable
internal expect fun rememberReducedMotion(): Boolean
