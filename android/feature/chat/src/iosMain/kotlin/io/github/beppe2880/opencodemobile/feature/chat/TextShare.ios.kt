package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

/** The activity sheet over the top view controller; on iPad it points at the middle of the screen. */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberTextShare(): (String) -> Unit = remember {
    { text: String ->
        val controller = topViewController()
        if (controller != null) {
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            sheet.popoverPresentationController?.let { popover ->
                val view = controller.view
                popover.sourceView = view
                view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
            }
            controller.presentViewController(sheet, animated = true, completion = null)
        }
    }
}
