package io.github.beppe2880.opencodemobile.core.data.stt

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.network.SpeechToTextClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSpeechToTextRepositoryTest {

    private val server = MockWebServer().apply { start() }
    private val keys = MapSettings()
    private val settings = FakeSettings(
        AppSettings(voiceEngine = VoiceEngine.Cloud, sttProvider = SttProvider.Custom, sttBaseUrl = server.url("/v1").toString()),
    )
    private var language: String? = "it-IT"
    private val repository = CloudSpeechToTextRepository(keys, settings, SpeechToTextClient(), { language })
    private val clip = VoiceClip(ByteArray(16_000))

    @After
    fun tearDown() = server.shutdown()

    private fun answer(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    @Test
    fun `keys are stored per provider and only their presence is exposed`() = runBlocking {
        assertEquals(emptySet<SttProvider>(), repository.savedKeys.value)

        repository.saveKey(SttProvider.OpenRouter, "  sk-or-secret  ")
        repository.saveKey(SttProvider.Groq, "gsk-secret")

        assertEquals(setOf(SttProvider.OpenRouter, SttProvider.Groq), repository.savedKeys.value)
        assertEquals("sk-or-secret", keys.getStringOrNull("key_openrouter"))
        // A new instance (app restart) finds them.
        assertEquals(
            setOf(SttProvider.OpenRouter, SttProvider.Groq),
            CloudSpeechToTextRepository(keys, settings, SpeechToTextClient(), {
                null
            }).savedKeys.value,
        )

        repository.saveKey(SttProvider.Groq, "   ")
        repository.removeKey(SttProvider.OpenRouter)
        assertEquals(emptySet<SttProvider>(), repository.savedKeys.value)
        assertTrue(keys.keys.isEmpty())
    }

    @Test
    fun `a built-in provider without a key fails before any request`() = runBlocking {
        settings.settings.value = settings.settings.value.copy(sttProvider = SttProvider.OpenRouter)

        assertTrue(repository.transcribe(clip).exceptionOrNull() is TranscriptionError.NoKey)
        assertTrue(repository.test().exceptionOrNull() is TranscriptionError.NoKey)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a custom server needs a usable address and may work without a key`() = runBlocking {
        settings.settings.value = settings.settings.value.copy(sttBaseUrl = "ftp://example.com/v1")
        assertTrue(repository.transcribe(clip).exceptionOrNull() is TranscriptionError.BadAddress)
        settings.settings.value = settings.settings.value.copy(sttBaseUrl = "")
        assertTrue(repository.transcribe(clip).exceptionOrNull() is TranscriptionError.BadAddress)
        assertEquals(0, server.requestCount)

        settings.settings.value = settings.settings.value.copy(sttBaseUrl = server.url("/v1/").toString())
        answer("""{"text":" ciao "}""")
        assertEquals("ciao", repository.transcribe(clip).getOrThrow())

        val request = server.takeRequest()
        assertEquals("/v1/audio/transcriptions", request.path)
        assertNull(request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue("default model", body.contains("name=\"model\"\r\nContent-Length: 9\r\n\r\nwhisper-1\r\n"))
        assertTrue("language hint", body.contains("name=\"language\"\r\nContent-Length: 2\r\n\r\nit\r\n"))
    }

    @Test
    fun `the saved key, the chosen model and no language hint when it is off`() = runBlocking {
        repository.saveKey(SttProvider.Custom, "local-key")
        settings.settings.value = settings.settings.value.copy(
            sttModels = mapOf(SttProvider.Custom to "Systran/faster-whisper-small"),
            sttLanguageHint = false,
        )
        answer("""{"text":"hello"}""")

        assertEquals("hello", repository.transcribe(clip).getOrThrow())

        val request = server.takeRequest()
        assertEquals("Bearer local-key", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("Systran/faster-whisper-small"))
        assertFalse(body.contains("name=\"language\""))
        // The clip goes as a WAV file with its header.
        assertTrue(body.contains("RIFF") && body.contains("WAVEfmt "))
    }

    @Test
    fun `provider errors keep their meaning`() = runBlocking {
        answer("""{"error":{"message":"Invalid API key"}}""", 401)
        answer("""{"error":{"message":"model not found"}}""", 404)

        assertTrue(repository.test().exceptionOrNull() is TranscriptionError.Unauthorized)
        val provider = repository.transcribe(clip).exceptionOrNull() as TranscriptionError.Provider
        assertEquals(404, provider.status)
        assertEquals("model not found", provider.detail)
    }

    @Test
    fun `language tags become ISO-639-1 codes`() {
        assertEquals("it", CloudSpeechToTextRepository.isoLanguage("it-IT"))
        assertEquals("pt", CloudSpeechToTextRepository.isoLanguage("pt_BR"))
        assertEquals("he", CloudSpeechToTextRepository.isoLanguage("iw"))
        assertEquals("zh", CloudSpeechToTextRepository.isoLanguage("zh-Hans-CN"))
        assertNull(CloudSpeechToTextRepository.isoLanguage("fil-PH"))
        assertNull(CloudSpeechToTextRepository.isoLanguage(""))
        assertNull(CloudSpeechToTextRepository.isoLanguage(null))
    }

    @Test
    fun `only OpenRouter lists models`() = runBlocking {
        assertEquals(emptyList<String>(), repository.listModels(SttProvider.Groq).getOrThrow())
        assertEquals(0, server.requestCount)
    }

    private class FakeSettings(initial: AppSettings) : SettingsRepository {
        override val settings = MutableStateFlow(initial)
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }
}
