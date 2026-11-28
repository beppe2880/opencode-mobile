package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAppCancel
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

@Composable
actual fun rememberDeviceAuthenticator(): DeviceAuthenticator = remember { LocalDeviceAuthenticator() }

/** Face ID or Touch ID, with the device passcode as fallback ([LAPolicyDeviceOwnerAuthentication]). */
private class LocalDeviceAuthenticator : DeviceAuthenticator {

    @OptIn(ExperimentalForeignApi::class)
    override fun isAvailable(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)

    // iOS shows only the reason: the title is the app name. The subtitle says why the app asks.
    override suspend fun authenticate(title: String, subtitle: String): DeviceAuthResult {
        if (!isAvailable()) return DeviceAuthResult.Unavailable
        val context = LAContext()
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { context.invalidate() }
            context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = subtitle) { success, error ->
                val result = if (success) DeviceAuthResult.Success else error.toResult()
                // The reply comes on a private queue.
                dispatch_async(dispatch_get_main_queue()) {
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        }
    }
}

private fun NSError?.toResult(): DeviceAuthResult = when (this?.code) {
    LAErrorUserCancel, LAErrorSystemCancel, LAErrorAppCancel -> DeviceAuthResult.Cancelled
    LAErrorPasscodeNotSet -> DeviceAuthResult.Unavailable
    else -> DeviceAuthResult.Failed
}
