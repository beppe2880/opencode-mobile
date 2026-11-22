package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
internal actual fun rememberTranscriptExport(
    onReady: () -> Unit,
    onFailure: () -> Unit,
    onCancelled: () -> Unit,
    onProgress: suspend (Long, Long) -> Unit,
): (TranscriptFile, Boolean) -> Unit {
    val failed = rememberUpdatedState(onFailure)
    val ready = rememberUpdatedState(onReady)
    val cancelled = rememberUpdatedState(onCancelled)
    val scope = rememberCoroutineScope()
    return { transcript, save ->
        scope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    // An OS-managed temporary directory per export, never reused by another conversation.
                    val directory = NSTemporaryDirectory() + "transcript-exports/" + NSUUID().UUIDString + "/"
                    NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
                    val path = directory + transcript.name
                    val content = NSString.create(string = transcript.content)
                    check(content.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null))
                    path
                }
                val host = topViewController()
                if (host == null) failed.value() else {
                    val url = NSURL.fileURLWithPath(path)
                    ready.value()
                    if (save) {
                        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                        host.presentViewController(picker, animated = true, completion = null)
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
            } catch (error: CancellationException) {
                cancelled.value()
                throw error
            } catch (_: Exception) {
                failed.value()
            }
        }
    }
}
