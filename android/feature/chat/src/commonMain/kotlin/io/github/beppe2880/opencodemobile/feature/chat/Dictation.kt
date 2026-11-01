package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecordingStyle
import kotlinx.coroutines.flow.StateFlow

/** Why dictation stopped without text. */
internal enum class DictationFailure { NoSpeech, Permission, Unavailable, Other }

/**
 * Voice input through the system speech recognizer: the words stream in as partial results
 * while the user speaks. The recognizer is created on the first use and released with the
 * composition.
 */
@Stable
internal interface Dictation {
    val available: Boolean
    val listening: Boolean

    /** Input loudness from 0 to 1 while listening. */
    val level: Float

    /** [onText] gets the whole text heard so far, each time it grows; [onFailure] when nothing was heard. */
    fun start(onText: (String) -> Unit, onFailure: (DictationFailure) -> Unit)

    /** Stops listening; the text heard so far is still delivered. */
    fun stop()

    /** Stops without delivering more text, e.g. when the prompt is sent. */
    fun cancel()
}

@Composable
internal expect fun rememberDictation(): Dictation

/** Microphone permission, asked on the first tap of the microphone. */
@Stable
internal interface MicPermission {
    fun granted(): Boolean

    /** Asks for the permission; the answer goes to the callback of [rememberMicPermission]. */
    fun request()
}

@Composable
internal expect fun rememberMicPermission(onResult: (Boolean) -> Unit): MicPermission

/** Appends dictated words to what was already typed. */
internal fun appendDictated(typed: String, spoken: String): String =
    if (typed.isBlank()) spoken else typed.trimEnd() + " " + spoken

/**
 * Puts a transcript in place of the selection of [text] (or at the cursor), with a space on each
 * side where a word would otherwise touch it; returns the new text and the cursor after it.
 */
internal fun insertSpoken(text: String, start: Int, end: Int, spoken: String): Pair<String, Int> {
    val from = start.coerceIn(0, text.length)
    val to = end.coerceIn(from, text.length)
    val before = text.substring(0, from)
    val after = text.substring(to)
    val words = spoken.trim()
    val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
    val trail = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
    val inserted = lead + words + trail
    return before + inserted + after to before.length + lead.length + words.length + trail.length
}

/** Elapsed recording time as `m:ss`. */
internal fun formatElapsed(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/**
 * Voice input as the composer sees it; null hides the microphone. [cloud] is the state of a
 * cloud recording, null with the system recognizer; [transcript] waits to be inserted at the
 * cursor. [level] is the system recognizer's loudness; a cloud recording's changes too often
 * for state and is read from [levels] while drawing.
 */
@Immutable
data class VoiceInput(
    val listening: Boolean,
    val level: Float,
    val cloud: CloudVoicePhase? = null,
    val provider: String = "",
    val transcript: PendingTranscript? = null,
    val levels: StateFlow<Float>? = null,
    val style: VoiceRecordingStyle = VoiceRecordingStyle.Waveform,
    val holdToRecord: Boolean = false,
) {
    /** A cloud recording or transcription shown inside the message box instead of the text. */
    val inBox: Boolean get() = cloud != null && style == VoiceRecordingStyle.Waveform &&
        (cloud is CloudVoicePhase.Recording || cloud == CloudVoicePhase.Transcribing)
}
