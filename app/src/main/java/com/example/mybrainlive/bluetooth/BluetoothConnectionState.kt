package com.example.mybrainlive.bluetooth

enum class ConnectionType {
    SPP,
    BLE
}

sealed interface BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState
    data object Scanning : BluetoothConnectionState
    data class Connecting(val deviceName: String) : BluetoothConnectionState
    data class Connected(
        val deviceName: String,
        val deviceAddress: String,
        val connectionType: ConnectionType = ConnectionType.SPP,
    ) : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}
