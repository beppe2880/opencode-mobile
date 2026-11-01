package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

/** What turns the voice of the microphone button into text. */
enum class VoiceEngine {
    /** The phone's own speech recognition, with words appearing while speaking. */
    System,

    /** A clip is recorded and sent to a cloud speech-to-text provider when the user stops. */
    Cloud,
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

/**
 * Microphone recording for cloud transcription. Platform implementations need the microphone
 * permission when [needsPermission].
 */
interface VoiceRecorder {
    val needsPermission: Boolean

    /**
     * Records until [stop] completes or [maxMs] is reached, reporting the elapsed time and the
     * input loudness (0 to 1) about ten times a second, and returns the clip. Cancelling the call
     * discards the recording. Throws when the microphone cannot be opened.
     */
    suspend fun record(stop: Deferred<Unit>, maxMs: Long = MAX_VOICE_CLIP_MS, onProgress: (elapsedMs: Long, level: Float) -> Unit): VoiceClip
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
