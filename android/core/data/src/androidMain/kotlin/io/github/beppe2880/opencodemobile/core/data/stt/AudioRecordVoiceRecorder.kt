package io.github.beppe2880.opencodemobile.core.data.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.beppe2880.opencodemobile.core.domain.VOICE_LEVEL_INTERVAL_MS
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceLevel
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecorder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Records 16 kHz mono PCM with [AudioRecord]; the audio stays in memory until it is sent or
 * dropped. The level is the RMS of each 40 ms chunk read.
 */
internal class AudioRecordVoiceRecorder : VoiceRecorder {
    override val needsPermission: Boolean = true

    private val _level = MutableStateFlow(0f)
    override val level: StateFlow<Float> = _level.asStateFlow()

    // The caller holds RECORD_AUDIO: the chat asks for it before recording.
    @SuppressLint("MissingPermission")
    override suspend fun record(stop: Deferred<Unit>, maxMs: Long, onProgress: (elapsedMs: Long) -> Unit): VoiceClip =
        withContext(Dispatchers.IO) {
            val rate = VoiceClip.SAMPLE_RATE
            val minBuffer = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuffer <= 0) throw IOException("Recording not supported")
            val chunk = ByteArray((rate * VOICE_LEVEL_INTERVAL_MS / 1000 * 2).toInt())
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer, chunk.size * 2),
                )
            } catch (e: IllegalArgumentException) {
                throw IOException("Microphone unavailable", e)
            }
            try {
                if (record.state != AudioRecord.STATE_INITIALIZED) throw IOException("Microphone unavailable")
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw IOException("Microphone busy")
                val maxBytes = maxMs * rate / 1000 * 2
                val pcm = ByteArrayOutputStream()
                while (!stop.isCompleted && pcm.size() < maxBytes) {
                    ensureActive()
                    val read = record.read(chunk, 0, minOf(chunk.size.toLong(), maxBytes - pcm.size()).toInt())
                    if (read < 0) throw IOException("Recording failed: $read")
                    if (read == 0) continue
                    pcm.write(chunk, 0, read)
                    _level.value = VoiceLevel.fromPcm16(chunk, read)
                    onProgress(pcm.size() / 2 * 1000L / rate)
                }
                VoiceClip(pcm.toByteArray(), rate)
            } finally {
                _level.value = 0f
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
                record.release()
            }
        }
}
