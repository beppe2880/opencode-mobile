package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal actual fun SystemBarsHidden(hidden: Boolean) {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window, hidden) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (hidden) {
            // A swipe from the edge shows the bars for a moment without leaving full screen.
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (hidden) controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
