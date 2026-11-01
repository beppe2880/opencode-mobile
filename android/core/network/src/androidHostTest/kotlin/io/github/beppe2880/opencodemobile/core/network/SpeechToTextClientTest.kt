package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.SttWireFormat
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechToTextClientTest {

    private val server = MockWebServer().apply { start() }
    private val client = SpeechToTextClient(boundary = { "BOUNDARY" })
    private val base = server.url("/v1").toString()

    /** Not a real WAV: printable bytes keep the golden body readable. */
    private val wav = "RIFFdemo".encodeToByteArray()

    @After
    fun tearDown() = server.shutdown()

    private fun answer(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    private fun request(format: SttWireFormat, language: String? = "en", key: String? = "sk-test") =
        SttRequest(baseUrl = base, format = format, apiKey = key, model = "whisper-large-v3-turbo", wav = wav, language = language)

    @Test
    fun `OpenAI-compatible providers get a multipart upload`() = runBlocking {
        answer("""{"text":"  hello from opencode mobile  ","usage":{"seconds":2}}""")

        assertEquals("hello from opencode mobile", client.transcribe(request(SttWireFormat.OpenAiMultipart)))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/audio/transcriptions", recorded.path)
        assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        assertEquals("multipart/form-data; boundary=BOUNDARY", recorded.getHeader("Content-Type"))
        assertEquals(
            "--BOUNDARY\r\n" +
                "Content-Disposition: form-data; name=\"model\"\r\n" +
                "Content-Length: 22\r\n" +
                "\r\n" +
                "whisper-large-v3-turbo\r\n" +
                "--BOUNDARY\r\n" +
                "Content-Disposition: form-data; name=\"language\"\r\n" +
                "Content-Length: 2\r\n" +
                "\r\n" +
                "en\r\n" +
                "--BOUNDARY\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n" +
                "Content-Type: audio/wav\r\n" +
                "Content-Length: 8\r\n" +
                "\r\n" +
                "RIFFdemo\r\n" +
                "--BOUNDARY--\r\n",
            recorded.body.readUtf8(),
        )
    }

    @Test
    fun `without a language or a key those fields are left out`() = runBlocking {
        answer("""{"text":"ok"}""")

        client.transcribe(request(SttWireFormat.OpenAiMultipart, language = null, key = null))

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
        assertFalse(recorded.body.readUtf8().contains("name=\"language\""))
    }

    @Test
    fun `OpenRouter gets JSON with the base64 WAV`() = runBlocking {
        answer("""{"text":"hello","usage":{"seconds":1.2,"cost":0.0001}}""")

        assertEquals("hello", client.transcribe(request(SttWireFormat.OpenRouterJson)))

        val recorded = server.takeRequest()
        assertEquals("/v1/audio/transcriptions", recorded.path)
        assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        assertTrue(recorded.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals(
            """{"model":"whisper-large-v3-turbo","input_audio":{"data":"UklGRmRlbW8=","format":"wav"},"language":"en"}""",
            recorded.body.readUtf8(),
        )
    }

    @Test
    fun `OpenRouter JSON leaves the language out to detect it`() {
        assertEquals(
            """{"model":"whisper-large-v3-turbo","input_audio":{"data":"UklGRmRlbW8=","format":"wav"}}""",
            SpeechToTextClient.openRouterJson(request(SttWireFormat.OpenRouterJson, language = null)).toString(),
        )
    }

    @Test
    fun `answers map to typed failures`() = runBlocking {
        val cases = listOf(
            401 to """{"error":{"message":"Incorrect API key provided: sk-***"}}""",
            403 to """{"error":"forbidden"}""",
            402 to """{"error":{"message":"Insufficient credits"}}""",
            429 to """{"error":{"message":"Rate limit reached"}}""",
            413 to "Request Entity Too Large",
        )
        cases.forEach { (code, body) -> answer(body, code) }

        val errors = cases.map { runCatching { client.transcribe(request(SttWireFormat.OpenAiMultipart)) }.exceptionOrNull() }

        assertTrue(errors[0] is TranscriptionError.Unauthorized)
        assertTrue(errors[1] is TranscriptionError.Unauthorized)
        assertTrue(errors[2] is TranscriptionError.RateLimited)
        assertTrue(errors[3] is TranscriptionError.RateLimited)
        assertTrue(errors[4] is TranscriptionError.TooLarge)
    }

    @Test
    fun `other errors carry the provider's short message`() = runBlocking {
        answer("""{"error":{"message":"Model mistralai/voxtral-small-24b-2507 does not exist","code":400}}""", 400)
        answer("""{"detail":[{"loc":["body","language"],"msg":"Invalid language alpha2 code","type":"value_error"}]}""", 422)
        answer("""{"message":"Service unavailable"}""", 503)
        answer("<html>Bad gateway</html>", 502)

        val details = List(4) {
            (
                runCatching {
                    client.transcribe(request(SttWireFormat.OpenRouterJson))
                }.exceptionOrNull() as TranscriptionError.Provider
                ).let { it.status to it.detail }
        }

        assertEquals(
            listOf(
                400 to "Model mistralai/voxtral-small-24b-2507 does not exist",
                422 to "Invalid language alpha2 code",
                503 to "Service unavailable",
                502 to null,
            ),
            details,
        )
        assertEquals(
            200,
            SpeechToTextClient.errorDetail("""{"error":{"message":"${"x".repeat(500)}"}}""")!!.length - 1,
        )
    }

    @Test
    fun `a success without text is a provider error`() = runBlocking {
        answer("""{"transcript":"wrong field"}""")
        answer("not json")

        repeat(2) {
            val error = runCatching { client.transcribe(request(SttWireFormat.OpenAiMultipart)) }.exceptionOrNull()
            assertTrue(error is TranscriptionError.Provider)
        }
    }

    @Test
    fun `an unreachable provider is a network failure`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = runCatching { client.transcribe(request(SttWireFormat.OpenRouterJson)) }.exceptionOrNull()

        assertTrue(error is TranscriptionError.Network)
    }

    @Test
    fun `OpenRouter lists its transcription models without a key`() = runBlocking {
        answer("""{"data":[{"id":"openai/whisper-large-v3-turbo","name":"Whisper"},{"id":"mistralai/voxtral-mini-transcribe"}]}""")

        assertEquals(listOf("openai/whisper-large-v3-turbo", "mistralai/voxtral-mini-transcribe"), client.openRouterModels(base))

        val recorded = server.takeRequest()
        assertEquals("/v1/models?output_modalities=transcription", recorded.path)
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `audio and keys go only over HTTPS, except to a server the user typed in`() {
        assertTrue(SpeechToTextClient.isAllowedBaseUrl("https://openrouter.ai/api/v1", allowHttp = false))
        assertFalse(SpeechToTextClient.isAllowedBaseUrl("http://openrouter.ai/api/v1", allowHttp = false))
        assertTrue(SpeechToTextClient.isAllowedBaseUrl("http://192.168.1.10:8000/v1", allowHttp = true))
        assertFalse(SpeechToTextClient.isAllowedBaseUrl("ftp://example.com", allowHttp = true))
        assertFalse(SpeechToTextClient.isAllowedBaseUrl("example.com/v1", allowHttp = true))
        assertFalse(SpeechToTextClient.isAllowedBaseUrl("", allowHttp = true))
        assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", SpeechToTextClient.transcriptionsUrl("https://api.groq.com/openai/v1/"))
    }
}
