package io.github.beppe2880.opencodemobile.core.data.stt

import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecorder
import io.github.beppe2880.opencodemobile.core.network.Wav
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfURL
import kotlin.time.Duration.Companion.milliseconds

/**
 * Records 16 kHz mono PCM with [AVAudioRecorder] into a temporary WAV file of the app, deleted
 * once the samples are read or the recording is cancelled.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AvVoiceRecorder : VoiceRecorder {
    override val needsPermission: Boolean = true

    override suspend fun record(stop: Deferred<Unit>, maxMs: Long, onProgress: (elapsedMs: Long, level: Float) -> Unit): VoiceClip {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, AVAudioSessionModeDefault, AVAudioSessionCategoryOptionDuckOthers, null)
        session.setActive(true, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + "voice-" + NSUUID().UUIDString + ".wav")
        val settings = mapOf<Any?, Any?>(
            AVFormatIDKey to kAudioFormatLinearPCM,
            AVSampleRateKey to VoiceClip.SAMPLE_RATE.toDouble(),
            AVNumberOfChannelsKey to 1,
            AVLinearPCMBitDepthKey to 16,
            AVLinearPCMIsFloatKey to false,
            AVLinearPCMIsBigEndianKey to false,
        )
        val recorder = AVAudioRecorder(uRL = url, settings = settings, error = null)
        try {
            recorder.meteringEnabled = true
            // Without a usable microphone (e.g. a simulator on a Mac without one) recording does not start.
            if (!recorder.prepareToRecord() || !recorder.recordForDuration(maxMs / 1000.0)) throw IllegalStateException("Microphone unavailable")
            while (!stop.isCompleted && recorder.isRecording()) {
                delay(PROGRESS_INTERVAL)
                recorder.updateMeters()
                val db = recorder.averagePowerForChannel(0u)
                onProgress((recorder.currentTime * 1000).toLong(), ((db - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0f, 1f))
            }
            recorder.stop()
            val pcm = withContext(Dispatchers.IO) {
                val data = NSData.dataWithContentsOfURL(url) ?: throw IllegalStateException("Recording missing")
                val bytes = data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt()) ?: ByteArray(0)
                Wav.pcm(bytes) ?: throw IllegalStateException("Unexpected recording format")
            }
            return VoiceClip(pcm, VoiceClip.SAMPLE_RATE)
        } finally {
            if (recorder.isRecording()) recorder.stop()
            NSFileManager.defaultManager.removeItemAtURL(url, null)
            session.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
        }
    }

    private companion object {
        val PROGRESS_INTERVAL = 100.milliseconds
        const val MIN_DB = -50f
        const val MAX_DB = -10f
    }
}
