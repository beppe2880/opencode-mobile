package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.SpeechToTextRepository
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecorder
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class VoiceInputViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val settings = FakeSettings(AppSettings(voiceEngine = VoiceEngine.Cloud, sttProvider = SttProvider.Groq))
    private val recorder = FakeRecorder()
    private val stt = FakeStt()
    private fun vm() = VoiceInputViewModel(settings, recorder, stt)

    @Test
    fun `the engine and provider follow the settings`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertEquals(VoiceEngine.Cloud, vm.state.value.engine)
        assertEquals(SttProvider.Groq, vm.state.value.provider)

        settings.settings.value = AppSettings(dictation = false)
        advanceUntilIdle()
        assertEquals(VoiceEngine.System, vm.state.value.engine)
        assertEquals(false, vm.state.value.enabled)
    }

    @Test
    fun `record, stop, transcribe, then the transcript waits for the message box`() = runTest(main.testDispatcher) {
        val vm = vm()
        stt.answers += Result.success("fix the login page")

        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()
        recorder.progress(2_500, 0.6f)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Recording(2_500, 0.6f), vm.state.value.phase)

        // A second tap while recording does not start another one.
        vm.onEvent(VoiceInputEvent.Start)
        assertEquals(1, recorder.starts)

        stt.gate = CompletableDeferred()
        vm.onEvent(VoiceInputEvent.Stop)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Transcribing, vm.state.value.phase)

        stt.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Idle, vm.state.value.phase)
        val transcript = vm.state.value.transcript!!
        assertEquals("fix the login page", transcript.text)
        assertEquals(2_500L, stt.clips.single().durationMs)

        vm.onEvent(VoiceInputEvent.TranscriptInserted(transcript.id + 1))
        assertEquals(transcript, vm.state.value.transcript)
        vm.onEvent(VoiceInputEvent.TranscriptInserted(transcript.id))
        assertNull(vm.state.value.transcript)
    }

    @Test
    fun `a failure keeps the recording until it is retried`() = runTest(main.testDispatcher) {
        val vm = vm()
        stt.answers += Result.failure(TranscriptionError.Network())
        stt.answers += Result.failure(TranscriptionError.Provider(400, "model not found"))
        stt.answers += Result.success("second time lucky")

        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()
        recorder.progress(4_000, 0.3f)
        vm.onEvent(VoiceInputEvent.Stop)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Failed(VoiceError.Network), vm.state.value.phase)

        vm.onEvent(VoiceInputEvent.Retry)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Failed(VoiceError.Provider, "model not found"), vm.state.value.phase)

        vm.onEvent(VoiceInputEvent.Retry)
        advanceUntilIdle()
        assertEquals("second time lucky", vm.state.value.transcript?.text)
        // The same clip each time, recorded once.
        assertEquals(1, recorder.starts)
        assertEquals(3, stt.clips.size)
        assertSame(stt.clips[0], stt.clips[2])
    }

    @Test
    fun `each provider error has its own message`() = runTest(main.testDispatcher) {
        val errors = listOf(
            TranscriptionError.NoKey() to VoiceError.NoKey,
            TranscriptionError.BadAddress() to VoiceError.BadAddress,
            TranscriptionError.Unauthorized() to VoiceError.Unauthorized,
            TranscriptionError.RateLimited() to VoiceError.RateLimited,
            TranscriptionError.TooLarge() to VoiceError.TooLarge,
        )
        val vm = vm()
        errors.forEach { (error, expected) ->
            stt.answers += Result.failure(error)
            vm.onEvent(VoiceInputEvent.Start)
            advanceUntilIdle()
            recorder.progress(1_000, 0f)
            vm.onEvent(VoiceInputEvent.Stop)
            advanceUntilIdle()
            assertEquals(CloudVoicePhase.Failed(expected), vm.state.value.phase)
        }
        stt.answers += Result.failure(TranscriptionError.Provider(500, null))
        vm.onEvent(VoiceInputEvent.Retry)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Failed(VoiceError.Provider, "HTTP 500"), vm.state.value.phase)
    }

    @Test
    fun `cancel drops the recording or the transcription in progress`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()
        recorder.progress(3_000, 0.5f)
        vm.onEvent(VoiceInputEvent.Cancel)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Idle, vm.state.value.phase)
        assertTrue(recorder.cancelled)
        assertTrue(stt.clips.isEmpty())

        stt.gate = CompletableDeferred()
        stt.answers += Result.success("never shown")
        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()
        recorder.progress(3_000, 0.5f)
        vm.onEvent(VoiceInputEvent.Stop)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Transcribing, vm.state.value.phase)
        vm.onEvent(VoiceInputEvent.Cancel)
        stt.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(CloudVoicePhase.Idle, vm.state.value.phase)
        assertNull(vm.state.value.transcript)
        // Nothing is left to retry.
        vm.onEvent(VoiceInputEvent.Retry)
        advanceUntilIdle()
        assertEquals(1, stt.clips.size)
    }

    @Test
    fun `a microphone that does not open is an error`() = runTest(main.testDispatcher) {
        recorder.failure = IOException("busy")
        val vm = vm()

        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()

        assertEquals(CloudVoicePhase.Failed(VoiceError.Microphone), vm.state.value.phase)
        // Retrying a clip that was never recorded does nothing.
        vm.onEvent(VoiceInputEvent.Retry)
        advanceUntilIdle()
        assertTrue(stt.clips.isEmpty())
    }

    @Test
    fun `a tap-tap or silence tells that nothing was heard`() = runTest(main.testDispatcher) {
        val vm = vm()
        val effects = async { vm.effect.first() }
        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()
        recorder.progress(100, 0f)
        vm.onEvent(VoiceInputEvent.Stop)
        advanceUntilIdle()
        assertEquals(VoiceInputEffect.NoSpeech, effects.await())
        assertTrue("too short to send", stt.clips.isEmpty())

        stt.answers += Result.success("   ")
        val second = async { vm.effect.first() }
        vm.onEvent(VoiceInputEvent.Start)
        advanceUntilIdle()
        recorder.progress(2_000, 0f)
        vm.onEvent(VoiceInputEvent.Stop)
        advanceUntilIdle()
        assertEquals(VoiceInputEffect.NoSpeech, second.await())
        assertEquals(CloudVoicePhase.Idle, vm.state.value.phase)
        assertNull(vm.state.value.transcript)
    }

    /** Records "silence" whose length is the last reported progress, until stopped. */
    private class FakeRecorder : VoiceRecorder {
        override val needsPermission = true
        var starts = 0
        var cancelled = false
        var failure: Exception? = null
        private var report: ((Long, Float) -> Unit)? = null
        private var elapsed = 0L

        fun progress(ms: Long, level: Float) {
            elapsed = ms
            report?.invoke(ms, level)
        }

        override suspend fun record(stop: Deferred<Unit>, maxMs: Long, onProgress: (Long, Float) -> Unit): VoiceClip {
            starts++
            failure?.let { throw it }
            elapsed = 0
            report = onProgress
            try {
                stop.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw e
            } finally {
                report = null
            }
            return VoiceClip(ByteArray((elapsed * VoiceClip.SAMPLE_RATE / 1000 * 2).toInt()))
        }
    }

    private class FakeStt : SpeechToTextRepository {
        val answers = ArrayDeque<Result<String>>()
        val clips = mutableListOf<VoiceClip>()
        var gate: CompletableDeferred<Unit>? = null
        override val savedKeys = MutableStateFlow(setOf(SttProvider.Groq))
        override suspend fun saveKey(provider: SttProvider, key: String) = Unit
        override suspend fun removeKey(provider: SttProvider) = Unit
        override suspend fun transcribe(clip: VoiceClip): Result<String> {
            clips += clip
            gate?.await()
            return answers.removeFirstOrNull() ?: awaitCancellation()
        }
        override suspend fun test() = Result.success(Unit)
        override suspend fun listModels(provider: SttProvider) = Result.success(emptyList<String>())
    }

    private class FakeSettings(initial: AppSettings) : SettingsRepository {
        override val settings = MutableStateFlow(initial)
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }
}
