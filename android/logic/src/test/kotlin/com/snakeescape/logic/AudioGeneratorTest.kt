package com.snakeescape.logic

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class AudioGeneratorTest {

    private fun writeWav(file: File, sampleRate: Int, samples: ShortArray) {
        file.parentFile.mkdirs()
        val byteRate = sampleRate * 2 // 16-bit mono = 2 bytes per sample
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size for PCM
            putShort(1) // AudioFormat 1 = PCM
            putShort(1) // NumChannels = 1 (mono)
            putInt(sampleRate)
            putInt(byteRate)
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray())
            putInt(dataSize)
        }

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                buffer.putShort(s)
            }
            fos.write(buffer.array())
        }
    }

    @Test
    fun generateAllAudioAssets() {
        val rawDir = File("../app/src/main/res/raw")
        val sampleRate = 22050

        // 1. sfx_tap: short crisp click / pop (50ms)
        val tapDuration = 0.05
        val tapCount = (sampleRate * tapDuration).toInt()
        val tapSamples = ShortArray(tapCount) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 90.0)
            val freq = 850.0 - t * 3000.0
            val wave = sin(2.0 * Math.PI * max(100.0, freq) * t)
            (wave * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        writeWav(File(rawDir, "sfx_tap.wav"), sampleRate, tapSamples)

        // 2. sfx_escape: cheerful rising whoosh chime (350ms)
        val escDuration = 0.35
        val escCount = (sampleRate * escDuration).toInt()
        val escSamples = ShortArray(escCount) { i ->
            val t = i.toDouble() / sampleRate
            val progress = t / escDuration
            val env = sin(progress * Math.PI).pow(0.5) * exp(-progress * 1.5)
            val freq = 420.0 + 700.0 * (progress * progress)
            val fundamental = sin(2.0 * Math.PI * freq * t)
            val harmonic = 0.35 * sin(4.0 * Math.PI * freq * t)
            val wave = (fundamental + harmonic) / 1.35
            (wave * env * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        writeWav(File(rawDir, "sfx_escape.wav"), sampleRate, escSamples)

        // 3. sfx_blocked: low thud / bonk (200ms)
        val blkDuration = 0.20
        val blkCount = (sampleRate * blkDuration).toInt()
        val blkSamples = ShortArray(blkCount) { i ->
            val t = i.toDouble() / sampleRate
            val progress = t / blkDuration
            val env = exp(-progress * 12.0)
            val freq = 170.0 - progress * 90.0
            val wave = sin(2.0 * Math.PI * freq * t)
            val clipped = wave.coerceIn(-0.8, 0.8) / 0.8
            (clipped * env * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        writeWav(File(rawDir, "sfx_blocked.wav"), sampleRate, blkSamples)

        // 4. sfx_win: 4-note victory major triad fanfare (C5, E5, G5, C6)
        val winDuration = 0.80
        val winCount = (sampleRate * winDuration).toInt()
        val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val winSamples = ShortArray(winCount) { i ->
            val t = i.toDouble() / sampleRate
            val noteIdx = min((t / 0.16).toInt(), 3)
            val noteStartTime = noteIdx * 0.16
            val noteT = t - noteStartTime
            val freq = notes[noteIdx]
            val env = exp(-noteT * if (noteIdx == 3) 4.5 else 9.0)
            val fundamental = sin(2.0 * Math.PI * freq * noteT)
            val chime = 0.25 * sin(4.0 * Math.PI * freq * noteT)
            val wave = (fundamental + chime) / 1.25
            (wave * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        writeWav(File(rawDir, "sfx_win.wav"), sampleRate, winSamples)

        // 5. bgm_loop: 6.0 second calm, cozy pentatonic ambient loop
        val bgmDuration = 6.0
        val bgmCount = (sampleRate * bgmDuration).toInt()
        // Pentatonic melody notes: C4 (261.63), D4 (293.66), E4 (329.63), G4 (392.00), A4 (440.00), C5 (523.25)
        val melodyEvents = listOf(
            0.0 to 261.63,
            0.75 to 329.63,
            1.50 to 392.00,
            2.25 to 440.00,
            3.00 to 523.25,
            3.75 to 440.00,
            4.50 to 392.00,
            5.25 to 329.63
        )
        val bgmSamples = ShortArray(bgmCount) { i ->
            val t = i.toDouble() / sampleRate

            // Warm ambient drone chords (C3 + G3)
            val drone1 = sin(2.0 * Math.PI * 130.81 * t)
            val drone2 = sin(2.0 * Math.PI * 196.00 * t) * 0.6
            val pad = (drone1 + drone2) * 0.22

            // Melody
            var melSum = 0.0
            for ((noteTime, noteFreq) in melodyEvents) {
                if (t >= noteTime) {
                    val dt = t - noteTime
                    val env = exp(-dt * 3.5)
                    val s = sin(2.0 * Math.PI * noteFreq * dt) + 0.2 * sin(4.0 * Math.PI * noteFreq * dt)
                    melSum += s * env * 0.35
                }
            }

            // Loop crossfade at boundaries to ensure zero click (10ms fade)
            val fadeTime = 0.05
            val fadeMultiplier = when {
                t < fadeTime -> t / fadeTime
                t > bgmDuration - fadeTime -> (bgmDuration - t) / fadeTime
                else -> 1.0
            }

            val mix = (pad + melSum) * fadeMultiplier
            (mix * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        writeWav(File(rawDir, "bgm_loop.wav"), sampleRate, bgmSamples)
    }
}
