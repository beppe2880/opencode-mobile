package io.github.beppe2880.opencodemobile.feature.chat

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.time.TimeSource

@Composable
internal actual fun rememberTranscriptExport(
    onReady: () -> Unit,
    onSaved: () -> Unit,
    onFailure: () -> Unit,
    onCancelled: () -> Unit,
    onProgress: suspend (Long, Long) -> Unit,
): (TranscriptFile, Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failed = rememberUpdatedState(onFailure)
    val ready = rememberUpdatedState(onReady)
    val saved = rememberUpdatedState(onSaved)
    val cancelled = rememberUpdatedState(onCancelled)
    val progress = rememberUpdatedState(onProgress)
    // Only a private file path enters saved state, never a potentially large transcript Bundle.
    val pending = rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data.takeIf { result.resultCode == Activity.RESULT_OK }
        val path = pending.value
        pending.value = null
        if (path != null) scope.launch {
            val file = File(path)
            try {
                if (uri != null) {
                    withContext(Dispatchers.IO) {
                        file.inputStream().use { input ->
                            checkNotNull(context.contentResolver.openOutputStream(uri)).use { input.copyTo(it) }
                        }
                    }
                    saved.value()
                } else if (result.resultCode == Activity.RESULT_OK) {
                    failed.value()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed.value()
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                    file.delete()
                    file.parentFile?.delete()
                }
            }
        } else if (result.resultCode == Activity.RESULT_OK) {
            failed.value()
        }
    }
    return { transcript, saveFile ->
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, "transcript-exports").apply { mkdirs() }
                    // On the next export, discard artifacts older than one day. Each share gets
                    // a unique URI: a previous recipient can never read a later export by title.
                    directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000L }?.forEach { old ->
                        if (old.isDirectory) old.listFiles()?.forEach { it.delete() }
                        old.delete()
                    }
                    val unique = File(directory, UUID.randomUUID().toString()).apply { mkdirs() }
                    val file = File(unique, transcript.name)
                    val content = transcript.content
                    val total = content.length.toLong()
                    progress.value(0, total)
                    var lastUpdate = TimeSource.Monotonic.markNow()
                    file.bufferedWriter().use { writer ->
                        var offset = 0
                        while (offset < content.length) {
                            currentCoroutineContext().ensureActive()
                            val count = minOf(64 * 1024, content.length - offset)
                            writer.write(content, offset, count)
                            offset += count
                            if (offset < content.length && lastUpdate.elapsedNow().inWholeMilliseconds >= 50) {
                                progress.value(offset.toLong(), total)
                                lastUpdate = TimeSource.Monotonic.markNow()
                            }
                        }
                    }
                    progress.value(total, total)
                    file
                }
                if (saveFile) {
                    pending.value = file.absolutePath
                    ready.value()
                    save.launch(
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = transcript.format.mimeType
                            putExtra(Intent.EXTRA_TITLE, file.name)
                        },
                    )
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.transcript-exports", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = transcript.format.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(file.name, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ready.value()
                    context.startActivity(Intent.createChooser(intent, null))
                }
            } catch (e: CancellationException) {
                cancelled.value()
                throw e
            } catch (_: Exception) {
                pending.value = null
                failed.value()
            }
        }
    }
}
