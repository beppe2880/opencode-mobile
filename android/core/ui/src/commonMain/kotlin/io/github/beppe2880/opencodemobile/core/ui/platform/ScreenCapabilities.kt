package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/** What the device's screen can do, whatever the size of the app window right now. */
@Immutable
data class ScreenCapabilities(
    /** The screen folds: a foldable, folded or not. */
    val foldable: Boolean,
    /** The app can get a wide window: a tablet, a large screen or an unfolded foldable. */
    val largeScreen: Boolean,
) {
    companion object {
        val Phone = ScreenCapabilities(foldable = false, largeScreen = false)
        val Tablet = ScreenCapabilities(foldable = false, largeScreen = true)
        val Foldable = ScreenCapabilities(foldable = true, largeScreen = true)
    }
}

/** What a platform can read about its screen; [capabilities] decides what they mean. */
data class ScreenSignals(
    /** The device reports a hinge (its angle sensor or the system feature for it). */
    val hinge: Boolean = false,
    /** The smallest width of the screen in its current configuration, in dp. */
    val smallestWidthDp: Int = 0,
    /** The shorter side of the largest window the app can get, in dp. */
    val largestWindowShortSideDp: Int = 0,
)

/** From this width a screen counts as large: where the app shows the chat beside the session list. */
const val LARGE_SCREEN_MIN_DP = 600

/** A hinge makes a foldable, which is a large screen even while folded; otherwise the size decides. */
fun ScreenSignals.capabilities(): ScreenCapabilities = ScreenCapabilities(
    foldable = hinge,
    largeScreen = hinge || smallestWidthDp >= LARGE_SCREEN_MIN_DP || largestWindowShortSideDp >= LARGE_SCREEN_MIN_DP,
)

/** The capabilities of this device's screen: Android reads its hinge and sizes, iOS tells iPads from iPhones. */
@Composable
expect fun rememberScreenCapabilities(): ScreenCapabilities
