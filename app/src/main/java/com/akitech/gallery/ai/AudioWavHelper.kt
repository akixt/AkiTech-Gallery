package com.akitech.gallery.ai

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioWavHelper {

    data class PreparedAudio(
        val bytes: ByteArray,
        val mimeType: String,
        val fileExtension: String,
    )

    fun preparePlayableAudio(rawBytes: ByteArray, rawMimeType: String): PreparedAudio {
        val mime = rawMimeType.trim().lowercase()
        val hasRiffHeader = rawBytes.size >= 12 &&
            rawBytes[0] == 'R'.code.toByte() &&
            rawBytes[1] == 'I'.code.toByte() &&
            rawBytes[2] == 'F'.code.toByte() &&
            rawBytes[3] == 'F'.code.toByte()

        val isRawPcm = mime.contains("l16") ||
            mime.contains("pcm") ||
            (mime.contains("wav") && !hasRiffHeader)

        if (isRawPcm && !hasRiffHeader) {
            val sampleRate = parseSampleRate(mime) ?: 24_000
            val channels = parseChannels(mime) ?: 1
            val wavBytes = wrapPcmAsWav(
                pcmBytes = rawBytes,
                sampleRate = sampleRate,
                channels = channels,
                bitsPerSample = 16,
            )
            return PreparedAudio(
                bytes = wavBytes,
                mimeType = "audio/wav",
                fileExtension = "wav",
            )
        }

        val ext = when {
            hasRiffHeader || mime.contains("wav") -> "wav"
            mime.contains("aac") -> "aac"
            mime.contains("ogg") -> "ogg"
            mime.contains("flac") -> "flac"
            else -> "mp3"
        }
        val normalizedMime = if (hasRiffHeader) "audio/wav" else rawMimeType.ifBlank { "audio/mpeg" }
        return PreparedAudio(
            bytes = rawBytes,
            mimeType = normalizedMime,
            fileExtension = ext,
        )
    }

    fun parseSampleRate(mimeType: String): Int? {
        val match = Regex("""rate=(\d+)""", RegexOption.IGNORE_CASE).find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 8_000..192_000 }
    }

    fun parseChannels(mimeType: String): Int? {
        val match = Regex("""channels=(\d+)""", RegexOption.IGNORE_CASE).find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..2 }
    }

    fun wrapPcmAsWav(
        pcmBytes: ByteArray,
        sampleRate: Int = 24_000,
        channels: Int = 1,
        bitsPerSample: Int = 16,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcmBytes.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())
            putInt(chunkSize)
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())
            putInt(16) // PCM subchunk1 size
            putShort(1.toShort()) // AudioFormat = 1 (PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())
            putInt(dataSize)
        }.array()

        return ByteArray(44 + dataSize).also { out ->
            System.arraycopy(header, 0, out, 0, 44)
            System.arraycopy(pcmBytes, 0, out, 44, dataSize)
        }
    }
}
