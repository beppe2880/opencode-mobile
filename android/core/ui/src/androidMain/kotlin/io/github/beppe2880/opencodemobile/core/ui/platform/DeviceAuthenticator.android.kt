package io.github.beppe2880.opencodemobile.core.ui.platform

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Any enrolled biometric (face or fingerprint, weak or strong), with the device credential as fallback. */
private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

@Composable
actual fun rememberDeviceAuthenticator(): DeviceAuthenticator {
    val context = LocalContext.current
    return remember(context) { BiometricDeviceAuthenticator(context) }
}

private class BiometricDeviceAuthenticator(private val context: Context) : DeviceAuthenticator {

    override fun isAvailable(): Boolean {
        if (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS) return true
        // Before Android 11 the check does not count the device credential alone.
        return context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
    }

    override suspend fun authenticate(title: String, subtitle: String): DeviceAuthResult {
        val activity = context.findActivity() ?: return DeviceAuthResult.Unavailable
        if (!isAvailable()) return DeviceAuthResult.Unavailable
        return suspendCancellableCoroutine { continuation ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(DeviceAuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(errorCode.toResult())
                }
                // A single failed attempt keeps the prompt open: the system lets the user retry.
            }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setConfirmationRequired(false)
                .build()
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info)
        }
    }
}

private fun Int.toResult(): DeviceAuthResult = when (this) {
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED,
    -> DeviceAuthResult.Cancelled
    BiometricPrompt.ERROR_NO_BIOMETRICS,
    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
    BiometricPrompt.ERROR_HW_NOT_PRESENT,
    -> DeviceAuthResult.Unavailable
    else -> DeviceAuthResult.Failed
}

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
