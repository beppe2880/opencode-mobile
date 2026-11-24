package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable

enum class TranscriptFormat(val extension: String, val mimeType: String) {
    Markdown("md", "text/markdown"),
    Json("json", "application/json"),
}

enum class ExportPhase { WaitingForServer, Downloading, Decoding, PreparingMessages, PreparingMarkdown, PreparingFile }

data class TranscriptFile(val title: String, val content: String, val format: TranscriptFormat) {
    val name: String get() = transcriptFileName(title, format)
}

/** Opens a native share sheet or save picker; a cancellation is not an error. */
@Composable
internal expect fun rememberTranscriptExport(
    onReady: () -> Unit,
    onSaved: () -> Unit,
    onFailure: () -> Unit,
    onCancelled: () -> Unit,
    onProgress: suspend (Long, Long) -> Unit,
): (TranscriptFile, Boolean) -> Unit

internal fun transcriptFileName(title: String, format: TranscriptFormat): String =
    title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim(' ', '.').take(80).ifBlank { "OpenCode" } + ".${format.extension}"
