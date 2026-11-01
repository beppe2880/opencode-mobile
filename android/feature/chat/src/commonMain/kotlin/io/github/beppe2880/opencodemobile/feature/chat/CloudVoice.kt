package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.SpeechToTextRepository
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecorder
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Where a cloud recording is: idle, recording, waiting for the provider, or failed with the clip kept. */
sealed interface CloudVoicePhase {
    data object Idle : CloudVoicePhase

    data class Recording(val elapsedMs: Long, val level: Float) : CloudVoicePhase

    data object Transcribing : CloudVoicePhase

    /** The recording is kept: [VoiceInputEvent.Retry] sends it again. [detail] is the provider's own message. */
    data class Failed(val error: VoiceError, val detail: String? = null) : CloudVoicePhase
}

enum class VoiceError { NoKey, BadAddress, Unauthorized, RateLimited, TooLarge, Network, Provider, Microphone }

/** A transcript waiting to be inserted at the cursor of the message box; [id] tells two apart. */
@Immutable
data class PendingTranscript(val id: Int, val text: String)

@Immutable
data class VoiceInputState(
    /** The microphone button is on (setting). */
    val enabled: Boolean = true,
    val engine: VoiceEngine = VoiceEngine.System,
    val provider: SttProvider = SttProvider.OpenRouter,
    val phase: CloudVoicePhase = CloudVoicePhase.Idle,
    val transcript: PendingTranscript? = null,
)

sealed interface VoiceInputEvent {
    /** Starts recording; the microphone permission is already granted. */
    data object Start : VoiceInputEvent

    /** Stops recording and sends the clip. */
    data object Stop : VoiceInputEvent

    /** Drops the recording, or the transcription in progress. */
    data object Cancel : VoiceInputEvent

    /** Sends the kept recording again after a failure. */
    data object Retry : VoiceInputEvent

    data class TranscriptInserted(val id: Int) : VoiceInputEvent
}

sealed interface VoiceInputEffect {
    /** Nothing was said, or the clip was too short to send. */
    data object NoSpeech : VoiceInputEffect
}

/**
 * Voice input through a cloud provider: tap to record, tap to stop, the transcript goes to the
 * message box and is never sent by itself. A failed clip is kept in memory until it is retried,
 * cancelled or replaced, and dropped with the chat.
 */
class VoiceInputViewModel(
    settings: SettingsRepository,
    private val recorder: VoiceRecorder,
    private val stt: SpeechToTextRepository,
) : BaseViewModel<VoiceInputState, VoiceInputEvent, VoiceInputEffect>(VoiceInputState()) {

    private var job: Job? = null
    private var stopSignal: CompletableDeferred<Unit>? = null
    private var clip: VoiceClip? = null
    private var transcripts = 0

    /** Written by the recorder from its own thread, read on the main thread. */
    private val progress = MutableStateFlow(0L to 0f)

    val needsPermission: Boolean get() = recorder.needsPermission

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                updateState { it.copy(enabled = s.dictation, engine = s.voiceEngine, provider = s.sttProvider) }
            }
        }
    }

    override fun onEvent(event: VoiceInputEvent) {
        when (event) {
            VoiceInputEvent.Start -> start()
            VoiceInputEvent.Stop -> stopSignal?.complete(Unit)
            VoiceInputEvent.Cancel -> {
                job?.cancel()
                job = null
                clip = null
                updateState { it.copy(phase = CloudVoicePhase.Idle) }
            }
            VoiceInputEvent.Retry -> {
                val kept = clip ?: return
                if (currentState.phase !is CloudVoicePhase.Failed) return
                job = viewModelScope.launch { transcribe(kept) }
            }
            is VoiceInputEvent.TranscriptInserted -> updateState { state ->
                if (state.transcript?.id == event.id) state.copy(transcript = null) else state
            }
        }
    }

    private fun start() {
        val phase = currentState.phase
        if (phase is CloudVoicePhase.Recording || phase == CloudVoicePhase.Transcribing) return
        job?.cancel()
        clip = null
        val stop = CompletableDeferred<Unit>().also { stopSignal = it }
        progress.value = 0L to 0f
        updateState { it.copy(phase = CloudVoicePhase.Recording(0, 0f)) }
        job = viewModelScope.launch {
            val recorded = try {
                coroutineScope {
                    val shown = launch { progress.collect { (ms, level) -> updateState { it.copy(phase = CloudVoicePhase.Recording(ms, level)) } } }
                    recorder.record(stop) { ms, level -> progress.value = ms to level }.also { shown.cancel() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                updateState { it.copy(phase = CloudVoicePhase.Failed(VoiceError.Microphone)) }
                return@launch
            } finally {
                if (stopSignal === stop) stopSignal = null
            }
            if (recorded.durationMs < MIN_CLIP_MS) {
                updateState { it.copy(phase = CloudVoicePhase.Idle) }
                sendEffect(VoiceInputEffect.NoSpeech)
            } else {
                transcribe(recorded)
            }
        }
    }

    private suspend fun transcribe(recorded: VoiceClip) {
        clip = recorded
        updateState { it.copy(phase = CloudVoicePhase.Transcribing) }
        stt.transcribe(recorded)
            .onSuccess { text ->
                clip = null
                if (text.isBlank()) {
                    updateState { it.copy(phase = CloudVoicePhase.Idle) }
                    sendEffect(VoiceInputEffect.NoSpeech)
                } else {
                    updateState { it.copy(phase = CloudVoicePhase.Idle, transcript = PendingTranscript(++transcripts, text)) }
                }
            }
            .onFailure { error ->
                val failure = when (error) {
                    is TranscriptionError.NoKey -> CloudVoicePhase.Failed(VoiceError.NoKey)
                    is TranscriptionError.BadAddress -> CloudVoicePhase.Failed(VoiceError.BadAddress)
                    is TranscriptionError.Unauthorized -> CloudVoicePhase.Failed(VoiceError.Unauthorized)
                    is TranscriptionError.RateLimited -> CloudVoicePhase.Failed(VoiceError.RateLimited)
                    is TranscriptionError.TooLarge -> CloudVoicePhase.Failed(VoiceError.TooLarge)
                    is TranscriptionError.Provider -> CloudVoicePhase.Failed(VoiceError.Provider, error.detail ?: "HTTP ${error.status}")
                    else -> CloudVoicePhase.Failed(VoiceError.Network)
                }
                updateState { it.copy(phase = failure) }
            }
    }

    private companion object {
        /** A tap on the button twice in a row records nothing worth sending. */
        const val MIN_CLIP_MS = 300L
    }
}
