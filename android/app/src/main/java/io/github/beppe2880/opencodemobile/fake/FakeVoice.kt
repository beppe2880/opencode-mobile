package io.github.beppe2880.opencodemobile.fake

import io.github.beppe2880.opencodemobile.core.domain.SpeechToTextRepository
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.VOICE_LEVEL_INTERVAL_MS
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecorder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The demo's voice input: a simulated microphone (no permission, no audio) and a fake provider
 * that answers with a sample transcript. Nothing ever reaches a speech-to-text provider, and
 * keys typed in the demo stay in memory.
 */
internal class FakeVoiceRecorder : VoiceRecorder {
    override val needsPermission: Boolean = false

    private val _level = MutableStateFlow(0f)
    override val level: StateFlow<Float> = _level.asStateFlow()

    override suspend fun record(stop: Deferred<Unit>, maxMs: Long, onProgress: (elapsedMs: Long) -> Unit): VoiceClip {
        var elapsed = 0L
        try {
            while (!stop.isCompleted && elapsed < maxMs) {
                delay(VOICE_LEVEL_INTERVAL_MS)
                elapsed += VOICE_LEVEL_INTERVAL_MS
                _level.value = syntheticLevel(elapsed)
                onProgress(elapsed)
            }
        } finally {
            _level.value = 0f
        }
        // Silent samples of the same length: only the duration matters to the demo.
        return VoiceClip(ByteArray((elapsed * VoiceClip.SAMPLE_RATE / 1000 * 2).toInt()))
    }

    private companion object {
        /** Speech-like loudness: syllables (~5 Hz) inside words (~0.7 Hz), with short pauses. */
        fun syntheticLevel(ms: Long): Float {
            val t = ms / 1000.0
            val word = sin(2 * PI * 0.7 * t)
            if (word < -0.6) return 0.04f
            val syllable = abs(sin(2 * PI * 2.6 * t + sin(t * 1.3)))
            return (0.15 + 0.7 * syllable * (0.6 + 0.4 * abs(word))).toFloat().coerceIn(0f, 1f)
        }
    }
}

internal class FakeSpeechToText : SpeechToTextRepository {
    private val keys = mutableMapOf<SttProvider, String>()
    private val saved = MutableStateFlow<Set<SttProvider>>(emptySet())
    override val savedKeys: StateFlow<Set<SttProvider>> = saved.asStateFlow()

    override suspend fun saveKey(provider: SttProvider, key: String) {
        if (key.isBlank()) return removeKey(provider)
        keys[provider] = key
        saved.value = keys.keys.toSet()
    }

    override suspend fun removeKey(provider: SttProvider) {
        keys.remove(provider)
        saved.value = keys.keys.toSet()
    }

    override suspend fun transcribe(clip: VoiceClip): Result<String> {
        delay(900)
        return Result.success("Add a dark mode toggle to the settings page and remember the choice")
    }

    override suspend fun test(): Result<Unit> {
        delay(600)
        return Result.success(Unit)
    }

    override suspend fun listModels(provider: SttProvider): Result<List<String>> = Result.success(
        if (provider == SttProvider.OpenRouter) provider.models + "openai/gpt-4o-mini-transcribe" else emptyList(),
    )
}
