package io.github.beppe2880.opencodemobile.core.ui.platform

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMotion

/** Follows the animator duration scale, so turning animations off applies without restarting the app. */
@Composable
actual fun rememberSystemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(resolver.animationsOff()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = resolver.animationsOff()
            }
        }
        // Previews and tests may have no settings provider to observe.
        val registered = runCatching {
            resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        }.isSuccess
        onDispose { if (registered) resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

private fun ContentResolver.animationsOff(): Boolean =
    runCatching { OcMotion.isReducedMotion(Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)) }.getOrDefault(false)
