package io.github.beppe2880.opencodemobile.core.data.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.beppe2880.opencodemobile.core.domain.VoiceClip
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecorder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.log10
import kotlin.math.sqrt

/** Records 16 kHz mono PCM with [AudioRecord]; the audio stays in memory until it is sent or dropped. */
internal class AudioRecordVoiceRecorder : VoiceRecorder {
    override val needsPermission: Boolean = true

    // The caller holds RECORD_AUDIO: the chat asks for it before recording.
    @SuppressLint("MissingPermission")
    override suspend fun record(stop: Deferred<Unit>, maxMs: Long, onProgress: (elapsedMs: Long, level: Float) -> Unit): VoiceClip =
        withContext(Dispatchers.IO) {
            val rate = VoiceClip.SAMPLE_RATE
            val minBuffer = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuffer <= 0) throw IOException("Recording not supported")
            val chunk = ByteArray(rate / 10 * 2)
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
                    onProgress(pcm.size() / 2 * 1000L / rate, loudness(chunk, read))
                }
                VoiceClip(pcm.toByteArray(), rate)
            } finally {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
                record.release()
            }
        }

    private companion object {
        const val MIN_DB = -50f
        const val MAX_DB = -10f

        /** RMS loudness of 16-bit samples, mapped from [MIN_DB]..[MAX_DB] to 0..1. */
        fun loudness(bytes: ByteArray, length: Int): Float {
            var sum = 0.0
            var count = 0
            var i = 0
            while (i + 1 < length) {
                val sample = ((bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)).toShort() / 32768.0
                sum += sample * sample
                count++
                i += 2
            }
            if (count == 0) return 0f
            val rms = sqrt(sum / count)
            val db = if (rms > 0) 20 * log10(rms).toFloat() else MIN_DB
            return ((db - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0f, 1f)
        }
    }
}
