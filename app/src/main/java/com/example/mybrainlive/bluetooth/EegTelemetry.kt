package com.example.mybrainlive.bluetooth

data class EegTelemetry(
    val deviceType: EegDeviceType,
    val signalQuality: Int = 0, // 0 = Perfect signal, 200 = Off head
    val attention: Int = 0,     // eSense Attention (0..100)
    val meditation: Int = 0,    // eSense Meditation (0..100)
    val rawWave: Int = 0,       // Raw EEG microvolts
    val blinkStrength: Int = 0,
    val eegBands: EegBands = EegBands(),
    val pluxChannels: List<Int> = emptyList(),
    val sequenceNumber: Int = 0,
    val rawBytesHex: String = "",
    val timestamp: Long = System.currentTimeMillis(),
) {
    val signalFitPercent: Int
        get() = when {
            signalQuality == 0 -> 100
            signalQuality >= 200 -> 0
            else -> maxOf(0, 100 - ((signalQuality * 100) / 200))
        }

    val isHeadsetConnected: Boolean
        get() = signalQuality < 200
}
