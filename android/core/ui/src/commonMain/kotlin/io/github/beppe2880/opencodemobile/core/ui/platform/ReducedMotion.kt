package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable

/** Whether the user asked the system for less motion: decorative animations then stay still. */
@Composable
expect fun rememberReducedMotion(): Boolean
