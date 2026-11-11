package io.github.beppe2880.opencodemobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad

/** No iPhone folds; iPads are large screens. */
@Composable
actual fun rememberScreenCapabilities(): ScreenCapabilities = remember {
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) ScreenCapabilities.Tablet else ScreenCapabilities.Phone
}
