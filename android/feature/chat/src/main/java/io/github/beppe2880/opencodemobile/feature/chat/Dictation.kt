package io.github.beppe2880.opencodemobile.feature.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Why dictation stopped without text. */
internal enum class DictationFailure { NoSpeech, Permission, Unavailable, Other }

/**
 * Voice input through the system speech recognizer (usually Google's): the words stream in
 * as partial results while the user speaks. The recognizer is created on the first use and
 * released with the composition.
 */
@Stable
internal class Dictation(private val context: Context) {
    val available: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    var listening by mutableStateOf(false)
        private set

    /** Input loudness from 0 to 1 while listening. */
    var level by mutableFloatStateOf(0f)
        private set

    private var recognizer: SpeechRecognizer? = null

    /** [onText] gets the whole text heard so far, each time it grows; [onFailure] when nothing was heard. */
    fun start(onText: (String) -> Unit, onFailure: (DictationFailure) -> Unit) {
        if (!available) return onFailure(DictationFailure.Unavailable)
        val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        var heard = false
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onRmsChanged(rmsdB: Float) {
                level = ((rmsdB - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0f, 1f)
            }

            override fun onEndOfSpeech() {
                level = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.bestText()?.let {
                    heard = true
                    onText(it)
                }
            }

            override fun onResults(results: Bundle?) {
                finish()
                results.bestText()?.let {
                    heard = true
                    onText(it)
                }
                if (!heard) onFailure(DictationFailure.NoSpeech)
            }

            override fun onError(error: Int) {
                finish()
                // Stopping after some words were heard ends with "no match" on some engines.
                if (heard && error in QUIET_ERRORS) return
                onFailure(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> DictationFailure.NoSpeech
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> DictationFailure.Permission
                        SpeechRecognizer.ERROR_CLIENT -> return
                        else -> DictationFailure.Other
                    },
                )
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        listening = true
        speech.startListening(intent)
    }

    /** Stops listening; the text heard so far is still delivered. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Stops without delivering more text, e.g. when the prompt is sent. */
    fun cancel() {
        recognizer?.cancel()
        finish()
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
        finish()
    }

    private fun finish() {
        listening = false
        level = 0f
    }

    private fun Bundle?.bestText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private companion object {
        const val MIN_DB = -2f
        const val MAX_DB = 10f
        val QUIET_ERRORS = setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT)
    }
}

@Composable
internal fun rememberDictation(): Dictation {
    val context = LocalContext.current
    val dictation = remember(context) { Dictation(context) }
    DisposableEffect(dictation) { onDispose { dictation.release() } }
    return dictation
}

/** Appends dictated words to what was already typed. */
internal fun appendDictated(typed: String, spoken: String): String =
    if (typed.isBlank()) spoken else typed.trimEnd() + " " + spoken

/** Voice input as the composer sees it; null hides the microphone. */
@Immutable
data class VoiceInput(val listening: Boolean, val level: Float)
