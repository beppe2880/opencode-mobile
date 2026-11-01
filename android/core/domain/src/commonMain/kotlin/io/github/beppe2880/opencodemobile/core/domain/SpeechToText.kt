package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/** What turns the voice of the microphone button into text. */
enum class VoiceEngine {
    /** The phone's own speech recognition, with words appearing while speaking. */
    System,

    /** A clip is recorded and sent to a cloud speech-to-text provider when the user stops. */
    Cloud,
}

/** How the message box shows a cloud recording. */
enum class VoiceRecordingStyle {
    /** The message box turns into the recorder: a live waveform, the time, cancel and confirm. */
    Waveform,

    /** A strip with the time above the message box, the microphone turns into a stop button. */
    Compact,
}

/** How a provider takes the audio. */
enum class SttWireFormat {
    /** `POST {base}/audio/transcriptions`, multipart `file` + `model` (+ `language`). */
    OpenAiMultipart,

    /** `POST {base}/audio/transcriptions`, JSON `{ model, input_audio: { data, format }, language }`. */
    OpenRouterJson,
}

/**
 * A cloud speech-to-text provider. [models] are curated defaults, the first one used until the
 * user picks another; [baseUrl] is null for [Custom], whose address the user enters.
 */
enum class SttProvider(val label: String, val baseUrl: String?, val format: SttWireFormat, val models: List<String>) {
    OpenRouter(
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        SttWireFormat.OpenRouterJson,
        listOf(
            "openai/whisper-large-v3-turbo",
            "openai/whisper-large-v3",
            "mistralai/voxtral-mini-transcribe",
            "mistralai/voxtral-small-24b-2507-stt",
        ),
    ),
    OpenAI("OpenAI", "https://api.openai.com/v1", SttWireFormat.OpenAiMultipart, listOf("gpt-4o-mini-transcribe", "gpt-4o-transcribe", "whisper-1")),
    Groq("Groq", "https://api.groq.com/openai/v1", SttWireFormat.OpenAiMultipart, listOf("whisper-large-v3-turbo", "whisper-large-v3")),
    Mistral(
        "Mistral",
        "https://api.mistral.ai/v1",
        SttWireFormat.OpenAiMultipart,
        listOf("voxtral-mini-latest", "voxtral-mini-transcribe-3", "voxtral-small-transcribe-3"),
    ),

    /** Any OpenAI-compatible server, e.g. a self-hosted Whisper; the only one allowed over plain HTTP. */
    Custom("Custom", null, SttWireFormat.OpenAiMultipart, listOf("whisper-1")),
    ;

    val defaultModel: String get() = models.first()
}

/** The model picked for [provider] in [AppSettings.sttModels], or its default. */
fun AppSettings.sttModel(provider: SttProvider = sttProvider): String = sttModels[provider]?.takeIf { it.isNotBlank() } ?: provider.defaultModel

/** A recording as 16-bit little-endian mono PCM. */
class VoiceClip(val pcm: ByteArray, val sampleRate: Int = SAMPLE_RATE) {
    val durationMs: Long get() = pcm.size / 2 * 1000L / sampleRate

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}

/** Longest recording: it stops by itself after this time. */
const val MAX_VOICE_CLIP_MS = 5 * 60 * 1000L

/** Why a cloud transcription failed; the message is never shown, the UI maps the type. */
sealed class TranscriptionError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** No API key saved for the chosen provider. */
    class NoKey : TranscriptionError()

    /** The custom provider has no usable address, or a built-in one would be reached over plain HTTP. */
    class BadAddress : TranscriptionError()

    /** The provider refused the key (401/403). */
    class Unauthorized : TranscriptionError()

    /** Too many requests or no credit left (429, 402). */
    class RateLimited : TranscriptionError()

    /** The recording is larger than the provider accepts. */
    class TooLarge : TranscriptionError()

    /** The provider could not be reached or did not answer in time. */
    class Network(cause: Throwable? = null) : TranscriptionError(cause = cause)

    /** Any other answer; [detail] is the provider's short error message, when it gave one. */
    class Provider(val status: Int, val detail: String?) : TranscriptionError("HTTP $status")
}

/** How often recorders measure the input level: about 25 times a second. */
const val VOICE_LEVEL_INTERVAL_MS = 40L

/**
 * Input loudness as the UI shows it: decibels relative to full scale mapped linearly from
 * [MIN_DB] (silence, 0) to [MAX_DB] (loud speech, 1).
 */
object VoiceLevel {
    const val MIN_DB = -50f
    const val MAX_DB = -10f

    fun fromDecibels(db: Float): Float = if (db.isNaN()) 0f else ((db - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0f, 1f)

    /** RMS of a full-scale-relative amplitude (0 to 1). */
    fun fromRms(rms: Double): Float = if (rms > 0) fromDecibels(20 * log10(rms).toFloat()) else 0f

    /** Level of the first [length] bytes of 16-bit little-endian mono PCM. */
    fun fromPcm16(bytes: ByteArray, length: Int = bytes.size): Float {
        var sum = 0.0
        var count = 0
        var i = 0
        val end = length.coerceAtMost(bytes.size)
        while (i + 1 < end) {
            val sample = ((bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)).toShort() / 32768.0
            sum += sample * sample
            count++
            i += 2
        }
        return if (count == 0) 0f else fromRms(sqrt(sum / count))
    }
}

/**
 * Microphone recording for cloud transcription. Platform implementations need the microphone
 * permission when [needsPermission].
 */
interface VoiceRecorder {
    val needsPermission: Boolean

    /**
     * Input level (0 to 1, see [VoiceLevel]) of the recording in progress, measured about every
     * [VOICE_LEVEL_INTERVAL_MS]; 0 when not recording.
     */
    val level: StateFlow<Float>

    /**
     * Records until [stop] completes or [maxMs] is reached, reporting the elapsed time while
     * [level] follows the voice, and returns the clip. Cancelling the call discards the
     * recording. Throws when the microphone cannot be opened.
     */
    suspend fun record(stop: Deferred<Unit>, maxMs: Long = MAX_VOICE_CLIP_MS, onProgress: (elapsedMs: Long) -> Unit): VoiceClip
}

/** Cloud speech-to-text with the provider and model chosen in [AppSettings]. */
interface SpeechToTextRepository {
    /** Providers with a saved API key. */
    val savedKeys: StateFlow<Set<SttProvider>>

    /** Stores the key of [provider] in the device's secure storage; a blank key removes it. */
    suspend fun saveKey(provider: SttProvider, key: String)

    suspend fun removeKey(provider: SttProvider)

    /** The text spoken in [clip]; failures are [TranscriptionError]. */
    suspend fun transcribe(clip: VoiceClip): Result<String>

    /** Sends a short silent clip with the current settings to check the key, address and model. */
    suspend fun test(): Result<Unit>

    /** Transcription models the provider lists, when it can be asked cheaply; empty otherwise. */
    suspend fun listModels(provider: SttProvider): Result<List<String>>
}
