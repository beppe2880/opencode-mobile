package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
