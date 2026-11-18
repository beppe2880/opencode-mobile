package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable

/** Not on iOS yet: its selection menu is built differently; the transcript keeps its own menu. */
@Composable
internal actual fun QuoteSelection(onQuote: (String) -> Unit, content: @Composable () -> Unit) = content()
