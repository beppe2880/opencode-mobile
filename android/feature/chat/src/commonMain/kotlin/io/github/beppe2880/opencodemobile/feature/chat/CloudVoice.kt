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
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecordingStyle
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Where a cloud recording is: idle, recording, waiting for the provider, or failed with the clip kept. */
sealed interface CloudVoicePhase {
    data object Idle : CloudVoicePhase

    /**
     * [elapsedMs] counts whole seconds, the precision the timer shows. [held] while the microphone
     * is held down (hold to record): releasing it transcribes, sliding away cancels.
     */
    data class Recording(val elapsedMs: Long, val held: Boolean = false) : CloudVoicePhase

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
    val style: VoiceRecordingStyle = VoiceRecordingStyle.Waveform,
    val holdToRecord: Boolean = false,
    val phase: CloudVoicePhase = CloudVoicePhase.Idle,
    val transcript: PendingTranscript? = null,
)

sealed interface VoiceInputEvent {
    /** Starts recording; the microphone permission is already granted. */
    data object Start : VoiceInputEvent

    /** Starts recording while the microphone is held down; [Stop] on release, [Cancel] when slid away. */
    data object Hold : VoiceInputEvent

    /** Stops recording and sends the clip. */
    data object Stop : VoiceInputEvent

    /**
     * Drops the recording, the transcription in progress or the failed clip at once; [Undo]
     * brings it back for [VOICE_UNDO_MS].
     */
    data object Cancel : VoiceInputEvent

    /** Sends the kept recording again after a failure. */
    data object Retry : VoiceInputEvent

    /** Transcribes the clip dropped by the last [Cancel], while it is still kept. */
    data object Undo : VoiceInputEvent

    data class TranscriptInserted(val id: Int) : VoiceInputEvent
}

sealed interface VoiceInputEffect {
    /** Nothing was said, or the clip was too short to send. */
    data object NoSpeech : VoiceInputEffect

    /** A recording was dropped: offer [VoiceInputEvent.Undo] for [VOICE_UNDO_MS]. */
    data object Discarded : VoiceInputEffect
}

/** How long a cancelled recording can be brought back. */
const val VOICE_UNDO_MS = 5_000L

/**
 * Voice input through a cloud provider: tap to record, tap to stop, the transcript goes to the
 * message box and is never sent by itself. A failed clip is kept in memory until it is retried,
 * cancelled or replaced, a cancelled one for [VOICE_UNDO_MS]; both are dropped with the chat.
 */
class VoiceInputViewModel(
    settings: SettingsRepository,
    private val recorder: VoiceRecorder,
    private val stt: SpeechToTextRepository,
) : BaseViewModel<VoiceInputState, VoiceInputEvent, VoiceInputEffect>(VoiceInputState()) {

    private var job: Job? = null
    private var stopSignal: CompletableDeferred<Unit>? = null

    /** The recording of a failure, kept for [VoiceInputEvent.Retry]. */
    private var clip: VoiceClip? = null

    /** The recording dropped by the last cancel, kept for [VoiceInputEvent.Undo]. */
    private var discarded: VoiceClip? = null
    private var discardExpiry: Job? = null

    /** The recording in progress was cancelled: when the recorder returns its clip goes to [discarded]. */
    private var cancelRecording = false
    private var transcripts = 0

    /** Written by the recorder from its own thread, read on the main thread. */
    private val elapsed = MutableStateFlow(0L)

    val needsPermission: Boolean get() = recorder.needsPermission

    /** Input level of the recording (0 to 1), about 25 times a second: read while drawing, never kept in state. */
    val level: StateFlow<Float> get() = recorder.level

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                updateState {
                    it.copy(
                        enabled = s.dictation,
                        engine = s.voiceEngine,
                        provider = s.sttProvider,
                        style = s.voiceRecordingStyle,
                        holdToRecord = s.voiceHoldToRecord,
                    )
                }
            }
        }
    }

    override fun onEvent(event: VoiceInputEvent) {
        when (event) {
            VoiceInputEvent.Start -> start(held = false)
            VoiceInputEvent.Hold -> start(held = true)
            VoiceInputEvent.Stop -> stopSignal?.complete(Unit)
            VoiceInputEvent.Cancel -> cancel()
            VoiceInputEvent.Retry -> {
                val kept = clip ?: return
                if (currentState.phase !is CloudVoicePhase.Failed) return
                job = viewModelScope.launch { transcribe(kept) }
            }
            VoiceInputEvent.Undo -> {
                val kept = discarded ?: return
                if (busy) return
                forgetDiscarded()
                job = viewModelScope.launch { transcribe(kept) }
            }
            is VoiceInputEvent.TranscriptInserted -> updateState { state ->
                if (state.transcript?.id == event.id) state.copy(transcript = null) else state
            }
        }
    }

    private val busy: Boolean get() = currentState.phase.let { it is CloudVoicePhase.Recording || it == CloudVoicePhase.Transcribing }

    private fun cancel() {
        when (currentState.phase) {
            is CloudVoicePhase.Recording -> {
                // The recorder hands the clip back once it has stopped, then it can be undone.
                cancelRecording = true
                stopSignal?.complete(Unit)
            }
            CloudVoicePhase.Transcribing, is CloudVoicePhase.Failed -> {
                job?.cancel()
                job = null
                clip?.let(::discard)
                clip = null
            }
            CloudVoicePhase.Idle -> return
        }
        updateState { it.copy(phase = CloudVoicePhase.Idle) }
    }

    private fun discard(recorded: VoiceClip) {
        discarded = recorded
        discardExpiry?.cancel()
        discardExpiry = viewModelScope.launch {
            delay(VOICE_UNDO_MS)
            discarded = null
        }
        sendEffect(VoiceInputEffect.Discarded)
    }

    private fun forgetDiscarded() {
        discardExpiry?.cancel()
        discardExpiry = null
        discarded = null
    }

    private fun start(held: Boolean) {
        if (busy) return
        val previous = job
        previous?.cancel()
        clip = null
        cancelRecording = false
        forgetDiscarded()
        val stop = CompletableDeferred<Unit>().also { stopSignal = it }
        elapsed.value = 0L
        updateState { it.copy(phase = CloudVoicePhase.Recording(0, held)) }
        job = viewModelScope.launch {
            // A recording cancelled a moment ago may still be closing the microphone.
            previous?.cancelAndJoin()
            val recorded = try {
                coroutineScope {
                    val shown = launch {
                        elapsed.collect { ms ->
                            val seconds = ms - ms % 1000
                            updateState { state ->
                                val phase = state.phase
                                if (phase is CloudVoicePhase.Recording && phase.elapsedMs != seconds) {
                                    state.copy(phase = phase.copy(elapsedMs = seconds))
                                } else {
                                    state
                                }
                            }
                        }
                    }
                    recorder.record(stop) { ms -> elapsed.value = ms }.also { shown.cancel() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (!cancelRecording) updateState { it.copy(phase = CloudVoicePhase.Failed(VoiceError.Microphone)) }
                cancelRecording = false
                return@launch
            } finally {
                if (stopSignal === stop) stopSignal = null
            }
            val cancelled = cancelRecording
            cancelRecording = false
            when {
                recorded.durationMs < MIN_CLIP_MS -> if (!cancelled) {
                    updateState { it.copy(phase = CloudVoicePhase.Idle) }
                    sendEffect(VoiceInputEffect.NoSpeech)
                }
                cancelled -> discard(recorded)
                else -> transcribe(recorded)
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
