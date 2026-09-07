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
 * Joue une tonalité douce (type bol chantant) générée synthétiquement.
 * Aucun fichier audio n'est requis : le PCM est généré à la volée.
 */
object MeditationTonePlayer {

    private const val SAMPLE_RATE = 22050

    /**
     * Joue un carillon doux en arrière-plan (non bloquant).
     * Deux fréquences harmoniques avec décroissance exponentielle.
     */
    fun playChime() {
        Thread {
            try {
                val pcm = generateChimePcm()
                val track = createTrack(pcm.size)
                track.write(pcm, 0, pcm.size)
                track.play()
                // Laisser le temps de jouer puis libérer
                val durationMs = (pcm.size / 2L) * 1000L / SAMPLE_RATE
                Thread.sleep(durationMs + 200)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Silencieux : l'audio ne doit jamais faire planter le flux EEG
            }
        }.apply { isDaemon = true }.start()
    }

    /**
     * Génère un PCM 16-bit mono : fondamentale 432 Hz + harmonique 648 Hz,
     * avec enveloppe de décroissance exponentielle (~2.5 s).
     */
    private fun generateChimePcm(): ShortArray {
        val durationSec = 2.5
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        val f1 = 432.0
        val f2 = 648.0 // quinte juste (f1 * 1.5)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-2.2 * t) // décroissance douce
            val attack = if (t < 0.03) t / 0.03 else 1.0 // évite le clic initial
            val sample = (sin(2.0 * PI * f1 * t) * 0.7 + sin(2.0 * PI * f2 * t) * 0.3)
            pcm[i] = (sample * envelope * attack * 24000).toInt()
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
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
                AudioManager.STREAM_NOTIFICATION,
                SAMPLE_RATE, channelConfig, encoding, bufferSize, AudioTrack.MODE_STATIC
            )
        }
    }
}
