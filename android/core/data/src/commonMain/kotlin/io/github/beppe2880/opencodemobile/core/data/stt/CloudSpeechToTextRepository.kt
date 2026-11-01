package io.github.beppe2880.opencodemobile.core.data.stt

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.SpeechToTextRepository
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.sttModel
import io.github.beppe2880.opencodemobile.core.network.SpeechToTextClient
import io.github.beppe2880.opencodemobile.core.network.SttRequest
import io.github.beppe2880.opencodemobile.core.network.Wav
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Cloud transcription with the provider, model and address of the settings. API keys live in
 * [keys], the device's secure storage (EncryptedSharedPreferences / Keychain, never backed up),
 * one entry per provider; they are only read to make a request and never logged or shown.
 */
internal class CloudSpeechToTextRepository(
    private val keys: Settings,
    private val settings: SettingsRepository,
    private val client: SpeechToTextClient,
    /** ISO-639-1 code of the app language, sent when the language hint is on. */
    private val language: () -> String?,
) : SpeechToTextRepository {

    private val mutex = Mutex()
    private val _savedKeys = MutableStateFlow(SttProvider.entries.filter { keys.hasKey(keyName(it)) }.toSet())
    override val savedKeys: StateFlow<Set<SttProvider>> = _savedKeys.asStateFlow()

    override suspend fun saveKey(provider: SttProvider, key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return removeKey(provider)
        mutex.withLock {
            withContext(Dispatchers.IO) { keys.putString(keyName(provider), trimmed) }
            _savedKeys.value += provider
        }
    }

    override suspend fun removeKey(provider: SttProvider) = mutex.withLock {
        withContext(Dispatchers.IO) { keys.remove(keyName(provider)) }
        _savedKeys.value -= provider
    }

    override suspend fun transcribe(clip: VoiceClip): Result<String> = request(clip.pcm, clip.sampleRate)

    override suspend fun test(): Result<Unit> = request(ByteArray(TEST_CLIP_BYTES), VoiceClip.SAMPLE_RATE).map { }

    override suspend fun listModels(provider: SttProvider): Result<List<String>> = failures {
        val base = provider.baseUrl
        if (provider != SttProvider.OpenRouter || base == null) emptyList() else client.openRouterModels(base)
    }

    private suspend fun request(pcm: ByteArray, sampleRate: Int): Result<String> = failures {
        val s = settings.settings.value
        val provider = s.sttProvider
        val base = provider.baseUrl ?: s.sttBaseUrl.trim()
        if (!SpeechToTextClient.isAllowedBaseUrl(base, allowHttp = provider == SttProvider.Custom)) throw TranscriptionError.BadAddress()
        val key = withContext(Dispatchers.IO) { keys.getStringOrNull(keyName(provider)) }
        // A self-hosted server may need no key.
        if (key == null && provider != SttProvider.Custom) throw TranscriptionError.NoKey()
        val wav = Wav.encode(pcm, sampleRate)
        if (wav.size > MAX_UPLOAD_BYTES) throw TranscriptionError.TooLarge()
        client.transcribe(
            SttRequest(
                baseUrl = base,
                format = provider.format,
                apiKey = key,
                model = s.sttModel(provider),
                wav = wav,
                language = if (s.sttLanguageHint) isoLanguage(language()) else null,
            ),
        )
    }

    private inline fun <T> failures(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: TranscriptionError) {
        Result.failure(e)
    }

    companion object {
        /** Under the 25 MB providers accept for one upload; five minutes of recording take about 9.6 MB. */
        const val MAX_UPLOAD_BYTES = 24 * 1024 * 1024

        /** Half a second of silence. */
        private const val TEST_CLIP_BYTES = VoiceClip.SAMPLE_RATE

        fun keyName(provider: SttProvider) = "key_" + provider.name.lowercase()

        /** The two-letter ISO-639-1 code of a language tag, with Java's legacy codes replaced; null when there is none. */
        fun isoLanguage(tag: String?): String? {
            val code = tag?.substringBefore('-')?.substringBefore('_')?.lowercase() ?: return null
            val iso = when (code) {
                "iw" -> "he"
                "in" -> "id"
                "ji" -> "yi"
                else -> code
            }
            return iso.takeIf { it.length == 2 && it.all { c -> c in 'a'..'z' } }
        }
    }
}
