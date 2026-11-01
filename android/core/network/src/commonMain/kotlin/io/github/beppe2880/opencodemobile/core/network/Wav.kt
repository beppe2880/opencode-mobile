package io.github.beppe2880.opencodemobile.core.network

/** WAV files of 16-bit little-endian PCM, as sent to speech-to-text providers. */
object Wav {
    private const val HEADER_SIZE = 44
    private const val BITS_PER_SAMPLE = 16

    /** A complete WAV file: the canonical 44-byte header followed by [pcm]. */
    fun encode(pcm: ByteArray, sampleRate: Int, channels: Int = 1): ByteArray {
        val blockAlign = channels * BITS_PER_SAMPLE / 8
        val out = ByteArray(HEADER_SIZE + pcm.size)
        var at = 0
        fun ascii(text: String) = text.forEach { out[at++] = it.code.toByte() }
        fun int32(value: Int) = repeat(4) { out[at++] = (value ushr (8 * it)).toByte() }
        fun int16(value: Int) = repeat(2) { out[at++] = (value ushr (8 * it)).toByte() }
        ascii("RIFF")
        int32(36 + pcm.size)
        ascii("WAVE")
        ascii("fmt ")
        int32(16)
        int16(1)
        int16(channels)
        int32(sampleRate)
        int32(sampleRate * blockAlign)
        int16(blockAlign)
        int16(BITS_PER_SAMPLE)
        ascii("data")
        int32(pcm.size)
        pcm.copyInto(out, HEADER_SIZE)
        return out
    }

    /**
     * The samples of the `data` chunk of a 16-bit PCM WAV file, skipping any other chunk (iOS
     * writes padding chunks before it); null when [wav] is not such a file.
     */
    fun pcm(wav: ByteArray): ByteArray? {
        if (wav.size < 12 || wav.ascii(0) != "RIFF" || wav.ascii(8) != "WAVE") return null
        var at = 12
        while (at + 8 <= wav.size) {
            val id = wav.ascii(at)
            val size = wav.int32(at + 4)
            val start = at + 8
            if (size < 0) return null
            if (id == "fmt " && (wav.int16(start) != 1 || wav.int16(start + 14) != BITS_PER_SAMPLE)) return null
            if (id == "data") return wav.copyOfRange(start, minOf(wav.size, start + size))
            // Chunks are padded to an even size.
            at = start + size + (size and 1)
        }
        return null
    }

    private fun ByteArray.ascii(at: Int) = (at until at + 4).map { this[it].toInt().toChar() }.joinToString("")

    private fun ByteArray.int16(at: Int) = (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.int32(at: Int) = int16(at) or (int16(at + 2) shl 16)
}
