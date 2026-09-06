package com.example.mybrainlive.bluetooth

data class EegBands(
    val delta: Int = 0,
    val theta: Int = 0,
    val lowAlpha: Int = 0,
    val highAlpha: Int = 0,
    val lowBeta: Int = 0,
    val highBeta: Int = 0,
    val lowGamma: Int = 0,
    val midGamma: Int = 0,
) {
    val totalPower: Int
        get() = delta + theta + lowAlpha + highAlpha + lowBeta + highBeta + lowGamma + midGamma
}
