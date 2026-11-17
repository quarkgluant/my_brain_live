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
 * Joue des tonalités synthétiques de biofeedback :
 * 1. Mode Carillon ponctuel (One-shot chime)
 * 2. Mode Son Continu Harmonique (Pitch-shifting continu doux & musical)
 */
object MeditationTonePlayer {

    private const val SAMPLE_RATE = 22050

    @Volatile
    private var isContinuousRunning = false

    @Volatile
    private var currentTargetScore = 50

    @Volatile
    private var isConcentrationMode = false

    private var continuousTrack: AudioTrack? = null

    fun playChime() {
        playTone(isConcentration = false)
    }

    fun playTone(isConcentration: Boolean) {
        Thread {
            try {
                val pcm = if (isConcentration) generateFocusPcm() else generateMeditationPcm()
                val track = createStaticTrack(pcm.size)
                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = (pcm.size / 2L) * 1000L / SAMPLE_RATE
                Thread.sleep(durationMs + 200)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Silencieux
            }
        }.apply { isDaemon = true }.start()
    }

    /**
     * Démarrage du son continu interactif
     */
    @Synchronized
    fun startContinuousTone(isConcentration: Boolean) {
        if (isContinuousRunning) return
        isContinuousRunning = true
        isConcentrationMode = isConcentration

        Thread {
            try {
                val minBuffer = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBuffer, 2048)

                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                continuousTrack = track
                track.play()

                var phase = 0.0
                var currentFreq = if (isConcentration) 350.0 else 380.0
                val frameSamples = 1024
                val pcmBuffer = ShortArray(frameSamples)

                while (isContinuousRunning) {
                    // Score cible (0..100)
                    val score = currentTargetScore.coerceIn(0, 100)

                    // Target frequency:
                    // Méditation: Score élevé => fréquence plus basse (432 Hz -> 172 Hz Om grave)
                    // Concentration: Score élevé => fréquence plus haute (300 Hz -> 750 Hz Focus brillant)
                    val targetFreq = if (isConcentrationMode) {
                        300.0 + (score / 100.0) * 450.0
                    } else {
                        432.0 - (score / 100.0) * 260.0
                    }

                    // Portamento doux
                    currentFreq = currentFreq * 0.90 + targetFreq * 0.10

                    val phaseIncrement = (2.0 * PI * currentFreq) / SAMPLE_RATE

                    for (i in 0 until frameSamples) {
                        phase += phaseIncrement
                        if (phase > 2.0 * PI) phase -= 2.0 * PI

                        // Synthèse harmonique douce (Fondamentale + 2e et 3e harmoniques légères)
                        val s1 = sin(phase) * 0.70
                        val s2 = sin(phase * 1.5) * 0.20
                        val s3 = sin(phase * 2.0) * 0.10

                        val sampleVal = (s1 + s2 + s3) * 16000.0 // Volume doux
                        pcmBuffer[i] = sampleVal.toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }

                    track.write(pcmBuffer, 0, frameSamples)
                }

                track.stop()
                track.release()
                continuousTrack = null
            } catch (_: Exception) {
                isContinuousRunning = false
            }
        }.apply { isDaemon = true }.start()
    }

    @Synchronized
    fun stopContinuousTone() {
        isContinuousRunning = false
    }

    fun updateContinuousScore(score: Int, isConcentration: Boolean) {
        currentTargetScore = score
        isConcentrationMode = isConcentration
        if (!isContinuousRunning) {
            startContinuousTone(isConcentration)
        }
    }

    fun isContinuousActive(): Boolean = isContinuousRunning

    private fun generateMeditationPcm(): ShortArray {
        val durationSec = 2.0
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        val f1 = 432.0
        val f2 = 648.0

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

    private fun generateFocusPcm(): ShortArray {
        val durationSec = 1.5
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        val f1 = 528.0
        val f2 = 792.0

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

    private fun createStaticTrack(pcmBytes: Int): AudioTrack {
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
