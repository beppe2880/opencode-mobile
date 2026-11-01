package io.github.beppe2880.opencodemobile.core.network

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WavTest {

    @Test
    fun headerDescribes16BitMonoPcmAt16kHz() {
        val pcm = byteArrayOf(1, 0, -1, -1, 0, 64)
        val wav = Wav.encode(pcm, sampleRate = 16_000)

        val expectedHeader = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            42, 0, 0, 0, // 36 + data size
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
            16, 0, 0, 0, // fmt chunk size
            1, 0, // PCM
            1, 0, // mono
            0x80.toByte(), 0x3E, 0, 0, // 16000 Hz
            0, 0x7D, 0, 0, // 32000 bytes per second
            2, 0, // block align
            16, 0, // bits per sample
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
            6, 0, 0, 0,
        )
        assertEquals(44 + pcm.size, wav.size)
        assertContentEquals(expectedHeader, wav.copyOfRange(0, 44))
        assertContentEquals(pcm, wav.copyOfRange(44, wav.size))
    }

    @Test
    fun samplesAreReadBackSkippingOtherChunks() {
        val pcm = ByteArray(10) { it.toByte() }
        assertContentEquals(pcm, Wav.pcm(Wav.encode(pcm, 16_000)))

        // Apple writes a padding chunk (FLLR) between fmt and data; odd sizes are padded.
        val encoded = Wav.encode(pcm, 16_000)
        val filler = "FLLR".encodeToByteArray() + byteArrayOf(3, 0, 0, 0, 9, 9, 9, 0)
        val withFiller = encoded.copyOfRange(0, 36) + filler + encoded.copyOfRange(36, encoded.size)
        assertContentEquals(pcm, Wav.pcm(withFiller))
    }

    @Test
    fun otherFilesAreRefused() {
        assertNull(Wav.pcm(ByteArray(0)))
        assertNull(Wav.pcm("not a wav file at all".encodeToByteArray()))
        // 32-bit float samples are not what the recorder asked for.
        val float = Wav.encode(ByteArray(4), 16_000).also { it[20] = 3; it[34] = 32 }
        assertNull(Wav.pcm(float))
    }
}
