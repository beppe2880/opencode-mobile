package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/** Read when the theme is first composed; a change applies the next time the app starts. */
@Composable
actual fun rememberSystemReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
