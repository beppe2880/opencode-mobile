package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable

/** Opens a native share sheet or save picker; a cancellation is not an error. */
@Composable
internal expect fun rememberMarkdownExport(onFailure: () -> Unit): (String, String, Boolean) -> Unit

internal fun markdownFileName(title: String): String =
    title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim(' ', '.').take(80).ifBlank { "OpenCode" } + ".md"
