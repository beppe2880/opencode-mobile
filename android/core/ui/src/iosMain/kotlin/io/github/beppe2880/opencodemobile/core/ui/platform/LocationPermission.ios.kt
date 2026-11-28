package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val wifiNameSupported: Boolean = false

@Composable
actual fun rememberLocationPermission(): LocationPermission = remember { LocationPermission { it(false) } }
