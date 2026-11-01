package io.github.beppe2880.opencodemobile.core.domain

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceLevelTest {

    /** A square wave of [amplitude] (0 to 1 of full scale) as 16-bit little-endian PCM: its RMS is the amplitude. */
    private fun square(amplitude: Double, samples: Int = 640): ByteArray {
        val value = (amplitude * 32767).toInt()
        return ByteArray(samples * 2).also { bytes ->
            for (i in 0 until samples) {
                val sample = if (i % 2 == 0) value else -value
                bytes[2 * i] = (sample and 0xFF).toByte()
                bytes[2 * i + 1] = (sample shr 8).toByte()
            }
        }
    }

    @Test
    fun decibelsMapLinearlyFromSilenceToLoudSpeech() {
        assertEquals(0f, VoiceLevel.fromDecibels(-50f))
        assertEquals(0.5f, VoiceLevel.fromDecibels(-30f))
        assertEquals(1f, VoiceLevel.fromDecibels(-10f))
        // Out of range values are clamped; iOS reports -160 dB for silence.
        assertEquals(0f, VoiceLevel.fromDecibels(-160f))
        assertEquals(1f, VoiceLevel.fromDecibels(0f))
        assertEquals(0f, VoiceLevel.fromDecibels(Float.NaN))
    }

    @Test
    fun rmsIsMeasuredInDecibelsOfFullScale() {
        assertEquals(0f, VoiceLevel.fromRms(0.0))
        assertEquals(0.5f, VoiceLevel.fromRms(10.0.pow(-30 / 20.0)), 0.001f)
        assertEquals(1f, VoiceLevel.fromRms(1.0))
    }

    @Test
    fun pcmLevelIsTheRmsOfItsSamples() {
        assertEquals(0f, VoiceLevel.fromPcm16(ByteArray(1280)))
        assertEquals(1f, VoiceLevel.fromPcm16(square(1.0)))
        assertEquals(0.5f, VoiceLevel.fromPcm16(square(10.0.pow(-30 / 20.0))), 0.01f)
        assertEquals(0.25f, VoiceLevel.fromPcm16(square(10.0.pow(-40 / 20.0))), 0.01f)
        // Only the bytes read count; an odd last byte is ignored.
        val loudThenSilent = square(1.0, samples = 4) + ByteArray(8)
        assertEquals(1f, VoiceLevel.fromPcm16(loudThenSilent, length = 9))
        assertEquals(0f, VoiceLevel.fromPcm16(ByteArray(0)))
    }
}
