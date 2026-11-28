package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** The outcome of asking the user to prove they own the device. */
enum class DeviceAuthResult {
    Success,

    /** The user closed the prompt, or it was interrupted (another app, the screen turned off). */
    Cancelled,

    /** Too many attempts, or the system refused; the prompt can be shown again later. */
    Failed,

    /** The device has no screen lock: neither biometrics nor a passcode is set up. */
    Unavailable,
}

/**
 * The system prompt for the face, a fingerprint or the device passcode: whichever the user set up
 * (the system picks among the enrolled biometrics; the passcode is always a fallback).
 */
@Stable
interface DeviceAuthenticator {
    /** Whether the device has a screen lock the app can ask for. */
    fun isAvailable(): Boolean

    /** Shows the system prompt with [title] and [subtitle] and waits for the user. */
    suspend fun authenticate(title: String, subtitle: String): DeviceAuthResult
}

@Composable
expect fun rememberDeviceAuthenticator(): DeviceAuthenticator
