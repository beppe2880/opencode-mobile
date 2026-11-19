package io.github.beppe2880.opencodemobile.feature.chat

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
internal actual fun rememberMarkdownExport(onFailure: () -> Unit): (String, String, Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failed = rememberUpdatedState(onFailure)
    // Only a private file path enters saved state, never a potentially large transcript Bundle.
    val pending = rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val path = pending.value
        pending.value = null
        if (uri != null && path != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    File(path).inputStream().use { input ->
                        checkNotNull(context.contentResolver.openOutputStream(uri)).use { input.copyTo(it) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed.value()
            }
        }
    }
    return { title, markdown, saveFile ->
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
                    File(unique, markdownFileName(title)).apply { writeText(markdown) }
                }
                if (saveFile) {
                    pending.value = file.absolutePath
                    save.launch(file.name)
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.transcript-exports", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(file.name, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                pending.value = null
                failed.value()
            }
        }
    }
}
