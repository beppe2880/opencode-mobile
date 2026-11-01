package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.time.measureTimedValue

/**
 * Transcribes a real recording with OpenRouter. Runs only when `OPENROUTER_API_KEY` and
 * `STT_LIVE_WAV` (a short 16 kHz mono 16-bit WAV saying "hello from opencode mobile") are set,
 * so never in CI. Prints the transcripts and latencies, never the key.
 */
class SpeechToTextLiveTest {

    @Test
    fun `OpenRouter transcribes a spoken clip with Whisper and Voxtral`() = runBlocking {
        val key = System.getenv("OPENROUTER_API_KEY")?.takeIf { it.isNotBlank() }
        val path = System.getenv("STT_LIVE_WAV")?.takeIf { it.isNotBlank() }
        assumeTrue("OPENROUTER_API_KEY and STT_LIVE_WAV not set", key != null && path != null)
        // Through the same path as a recording: samples out of the file, then the app's own WAV.
        val pcm = Wav.pcm(File(path!!).readBytes()) ?: error("$path is not a 16-bit PCM WAV")
        val wav = Wav.encode(pcm, 16_000)
        val provider = SttProvider.OpenRouter
        val client = SpeechToTextClient()

        val models = listOf("openai/whisper-large-v3-turbo", "mistralai/voxtral-mini-transcribe") + provider.models.drop(1)
        models.distinct().forEach { model ->
            val (text, took) = measureTimedValue {
                client.transcribe(SttRequest(provider.baseUrl!!, provider.format, key, model, wav, language = "en"))
            }
            println("STT live: $model -> \"$text\" in ${took.inWholeMilliseconds} ms")
            val words = text.lowercase().filter { it.isLetter() || it == ' ' }
            assertTrue("$model heard \"$text\"", "hello" in words && "mobile" in words)
        }
    }
}
