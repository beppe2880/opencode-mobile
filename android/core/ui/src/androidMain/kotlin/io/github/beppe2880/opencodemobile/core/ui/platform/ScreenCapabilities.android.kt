package io.github.beppe2880.opencodemobile.core.ui.platform

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Foldables are told by their hinge (Android 11+), which they report folded or not; large screens
 * by their smallest width or the largest window they allow. The device state API that lists fold
 * postures is a system API, and Jetpack WindowManager is not a dependency, so neither is read.
 */
@Composable
actual fun rememberScreenCapabilities(): ScreenCapabilities {
    val context = LocalContext.current
    val smallestWidthDp = LocalConfiguration.current.smallestScreenWidthDp
    return remember(context, smallestWidthDp) {
        ScreenSignals(
            hinge = hasHinge(context),
            smallestWidthDp = smallestWidthDp,
            largestWindowShortSideDp = largestWindowShortSideDp(context),
        ).capabilities()
    }
}

private fun hasHinge(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    return runCatching {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE) ||
            context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE) != null
    }.getOrDefault(false)
}

/** The shorter side of the largest window the app can get, in dp; 0 before Android 11. */
private fun largestWindowShortSideDp(context: Context): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0
    return runCatching {
        val bounds = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds ?: return 0
        (minOf(bounds.width(), bounds.height()) / context.resources.displayMetrics.density).toInt()
    }.getOrDefault(0)
}
