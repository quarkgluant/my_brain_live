package com.example.mybrainlive.bluetooth

data class EegDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean = false,
    val rssi: Int? = null,
    val deviceType: EegDeviceType = EegDeviceType.detectType(name),
) {
    val isEegDevice: Boolean
        get() = deviceType != EegDeviceType.GENERIC

    // Backwards compatibility for PluxDevice usages
    val isPluxDevice: Boolean
        get() = deviceType == EegDeviceType.PLUX
}


