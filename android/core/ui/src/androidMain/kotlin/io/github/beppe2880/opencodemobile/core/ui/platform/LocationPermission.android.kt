package io.github.beppe2880.opencodemobile.core.ui.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

actual val wifiNameSupported: Boolean = true

@Composable
actual fun rememberLocationPermission(): LocationPermission {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        pending[0]?.invoke(results[Manifest.permission.ACCESS_FINE_LOCATION] == true)
        pending[0] = null
    }
    return remember(context, launcher) {
        LocationPermission { onResult ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                onResult(true)
            } else {
                pending[0] = onResult
                // Android 12 and later grant the precise location only when the approximate one is asked too.
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }
}
