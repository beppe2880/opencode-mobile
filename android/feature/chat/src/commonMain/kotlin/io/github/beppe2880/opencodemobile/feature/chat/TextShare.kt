package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable

/** Hands plain text to the system share sheet: Android's chooser, iOS's activity sheet. */
@Composable
internal expect fun rememberTextShare(): (String) -> Unit
