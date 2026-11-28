package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Whether the app can read the name of the Wi-Fi network: on Android with the location permission.
 * iOS needs an entitlement the app does not have yet.
 */
expect val wifiNameSupported: Boolean

/** Asks for the location permission the system requires to tell the Wi-Fi network name. */
@Stable
fun interface LocationPermission {
    /** Calls [onResult] with whether it is granted, asking the user first when it is not yet. */
    fun request(onResult: (Boolean) -> Unit)
}

@Composable
expect fun rememberLocationPermission(): LocationPermission
