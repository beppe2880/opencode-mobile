package io.github.beppe2880.opencodemobile.core.data.stt

/**
 * Keeps a voice recording going while the app is in the background: the app holds the
 * microphone with a foreground service. [start] runs once the microphone records; [onStop] ends
 * the recording from outside the app (the Stop action of its notification). The returned
 * function runs when the recording ends.
 */
fun interface BackgroundRecording {
    fun start(onStop: () -> Unit): () -> Unit
}
