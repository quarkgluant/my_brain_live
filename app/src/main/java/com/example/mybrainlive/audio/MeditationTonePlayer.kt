package com.example.mybrainlive.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Joue des tonalités synthétiques de biofeedback (bol chantant 432 Hz ou son focus 528 Hz).
 * Utilise le flux audio MÉDIA (STREAM_MUSIC) pour garantir une audition maximale.
 */
object MeditationTonePlayer {

    private const val SAMPLE_RATE = 22050

    fun playChime() {
        playTone(isConcentration = false)
    }

    fun playTone(isConcentration: Boolean) {
        Thread {
            try {
                val pcm = if (isConcentration) generateFocusPcm() else generateMeditationPcm()
                val track = createTrack(pcm.size)
                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = (pcm.size / 2L) * 1000L / SAMPLE_RATE
                Thread.sleep(durationMs + 200)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Silencieux : évite tout crash du flux EEG
            }
        }.apply { isDaemon = true }.start()
    }

    /**
     * Tonalité de Méditation (432 Hz - Bol tibétain résonnant)
     */
    private fun generateMeditationPcm(): ShortArray {
        val durationSec = 2.0
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        val f1 = 432.0 // Fréquence fondamentale
        val f2 = 648.0 // Quinte juste (1.5 * f1)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-1.8 * t)
            val attack = if (t < 0.04) t / 0.04 else 1.0
            val sample = (sin(2.0 * PI * f1 * t) * 0.7 + sin(2.0 * PI * f2 * t) * 0.3)
            pcm[i] = (sample * envelope * attack * 28000).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return pcm
    }

    /**
     * Tonalité de Concentration / Focus (528 Hz - Carillon clair)
     */
    private fun generateFocusPcm(): ShortArray {
        val durationSec = 1.5
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        val f1 = 528.0 // Fréquence Focus Solfeggio
        val f2 = 792.0 // Harmonique supérieure (1.5 * f1)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-2.5 * t)
            val attack = if (t < 0.02) t / 0.02 else 1.0
            val sample = (sin(2.0 * PI * f1 * t) * 0.8 + sin(2.0 * PI * f2 * t) * 0.2)
            pcm[i] = (sample * envelope * attack * 29000).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return pcm
    }

    private fun createTrack(pcmBytes: Int): AudioTrack {
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = maxOf(
            pcmBytes,
            AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding)
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(channelConfig)
                        .setEncoding(encoding)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, channelConfig, encoding, bufferSize, AudioTrack.MODE_STATIC
            )
        }
    }
}
