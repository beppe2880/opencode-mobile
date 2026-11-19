package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberMarkdownExport(onFailure: () -> Unit): (String, String, Boolean) -> Unit {
    val failed = rememberUpdatedState(onFailure)
    return { title, markdown, save ->
        val host = topViewController()
        // An OS-managed temporary directory per export, never reused by another conversation.
        val directory = NSTemporaryDirectory() + "transcript-exports/" + NSUUID().UUIDString + "/"
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val path = directory + markdownFileName(title)
        val written = NSString.create(string = markdown).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (host == null || !written) failed.value() else {
            val url = NSURL.fileURLWithPath(path)
            if (save) {
                host.presentViewController(UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true), animated = true, completion = null)
            } else {
                val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
                sheet.popoverPresentationController?.let { popover ->
                    val view = host.view
                    popover.sourceView = view
                    view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
                }
                host.presentViewController(sheet, animated = true, completion = null)
            }
        }
    }
}
