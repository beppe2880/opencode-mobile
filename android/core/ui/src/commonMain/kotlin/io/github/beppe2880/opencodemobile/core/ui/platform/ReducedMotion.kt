package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable

/**
 * Whether the system asks for less motion: animations off on Android (animator duration scale 0,
 * also set by "Remove animations"), Reduce Motion on iOS. [io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme]
 * combines it with the Animations setting (`rememberMotionConfig`); read `OcTheme.motion` instead of calling this.
 */
@Composable
expect fun rememberSystemReducedMotion(): Boolean
