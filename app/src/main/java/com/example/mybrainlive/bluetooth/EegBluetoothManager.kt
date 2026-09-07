package com.example.mybrainlive.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EegBluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "EegBluetoothManager"

        // Standard SPP (Serial Port Profile) UUID for Bluetooth devices (PLUX, MindWave, BrainLink)
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Client Characteristic Configuration Descriptor for BLE notifications
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        // Known BLE Services and Characteristics for BrainLink Lite V2.0 / Serial BLE
        val BRAINLINK_BLE_SERVICE: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val BRAINLINK_BLE_CHAR: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

        val NORDIC_UART_SERVICE: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NORDIC_UART_CHAR_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        // PLUX / BITalino protocol commands
        val COMMAND_PLUX_START = byteArrayOf(0x01.toByte())
        val COMMAND_PLUX_STOP = byteArrayOf(0x00.toByte())
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<EegDevice>>(emptyList())
    val scannedDevices: StateFlow<List<EegDevice>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<EegDevice>>(emptyList())
    val pairedDevices: StateFlow<List<EegDevice>> = _pairedDevices.asStateFlow()

    private val _latestTelemetry = MutableStateFlow<EegTelemetry?>(null)
    val latestTelemetry: StateFlow<EegTelemetry?> = _latestTelemetry.asStateFlow()

    private val _totalBytesReceived = MutableStateFlow(0L)
    val totalBytesReceived: StateFlow<Long> = _totalBytesReceived.asStateFlow()

    private val _bytesPerSecond = MutableStateFlow(0)
    val bytesPerSecond: StateFlow<Int> = _bytesPerSecond.asStateFlow()

    private val _logEvents = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val logEvents: SharedFlow<String> = _logEvents.asSharedFlow()

    private var currentConnectedDeviceType: EegDeviceType = EegDeviceType.GENERIC
    private var neuroSkyParser: NeuroSkyParser? = null

    // SPP Socket connections
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // BLE GATT connections
    private var bluetoothGatt: BluetoothGatt? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private var throughputJob: Job? = null
    private var bleConnectTimeoutJob: Job? = null

    private var isReceiverRegistered = false
    private var isBleScanning = false

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = try {
                if (hasConnectPermission()) {
                    device.name ?: result.scanRecord?.deviceName ?: "Unknown BLE Device"
                } else "Unknown BLE Device"
            } catch (_: Exception) { "Unknown BLE Device" }

            val eegDevice = EegDevice(
                name = name,
                address = device.address,
                isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                rssi = result.rssi,
                deviceType = EegDeviceType.detectType(name),
            )

            val currentList = _scannedDevices.value.toMutableList()
            if (currentList.none { it.address == device.address }) {
                currentList.add(eegDevice)
                _scannedDevices.value = currentList
                log("Discovered BLE device: ${eegDevice.name} [${eegDevice.deviceType.displayName}]")
            }
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    device?.let { dev ->
                        val name = try {
                            if (hasConnectPermission()) dev.name ?: "Unknown Device" else "Unknown Device"
                        } catch (_: Exception) {
                            "Unknown Device"
                        }
                        val eegDevice = EegDevice(
                            name = name,
                            address = dev.address,
                            isPaired = dev.bondState == BluetoothDevice.BOND_BONDED,
                            rssi = if (rssi != Short.MIN_VALUE.toInt()) rssi else null,
                            deviceType = EegDeviceType.detectType(name),
                        )

                        val currentList = _scannedDevices.value.toMutableList()
                        if (currentList.none { it.address == dev.address }) {
                            currentList.add(eegDevice)
                            _scannedDevices.value = currentList
                            log("Discovered device: ${eegDevice.name} [${eegDevice.deviceType.displayName}]")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if ((_connectionState.value is BluetoothConnectionState.Scanning) && !isBleScanning) {
                        _connectionState.value = BluetoothConnectionState.Disconnected
                    }
                    log("Bluetooth classic discovery finished.")
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val devName = try { gatt.device.name ?: gatt.device.address } catch (_: Exception) { gatt.device.address }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleConnectTimeoutJob?.cancel()
                log("BLE GATT Connected to $devName. Discovering services...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleConnectTimeoutJob?.cancel()
                log("BLE GATT Disconnected from $devName.")
                _connectionState.value = BluetoothConnectionState.Disconnected
                cleanupGatt()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val devName = try { gatt.device.name ?: gatt.device.address } catch (_: Exception) { gatt.device.address }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("BLE GATT Services discovered for $devName. Finding notification characteristic...")

                var targetChar: BluetoothGattCharacteristic? = null

                // 1. Search for BrainLink / HM-10 service & characteristic
                val brainlinkService = gatt.getService(BRAINLINK_BLE_SERVICE)
                if (brainlinkService != null) {
                    targetChar = brainlinkService.getCharacteristic(BRAINLINK_BLE_CHAR)
                }

                // 2. Search for Nordic UART service
                if (targetChar == null) {
                    val nordicService = gatt.getService(NORDIC_UART_SERVICE)
                    if (nordicService != null) {
                        targetChar = nordicService.getCharacteristic(NORDIC_UART_CHAR_TX)
                    }
                }

                // 3. Fallback: Search all services for any notify/indicate characteristic
                if (targetChar == null) {
                    for (service in gatt.services) {
                        for (charac in service.characteristics) {
                            if (((charac.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) ||
                                ((charac.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)) {
                                targetChar = charac
                                break
                            }
                        }
                        if (targetChar != null) break
                    }
                }

                if (targetChar != null) {
                    log("Found BLE notification characteristic: ${targetChar.uuid}. Subscribing...")
                    notifyCharacteristic = targetChar
                    gatt.setCharacteristicNotification(targetChar, true)

                    val descriptor = targetChar.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(
                                descriptor,
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }

                    _connectionState.value = BluetoothConnectionState.Connected(
                        deviceName = devName,
                        deviceAddress = gatt.device.address,
                        connectionType = ConnectionType.BLE
                    )
                    log("Subscribed to $devName BLE notifications successfully.")
                    startThroughputCalculator()

                } else {
                    log("No notify/indicate BLE characteristic found on $devName.")
                    _connectionState.value = BluetoothConnectionState.Error("No BLE serial characteristic found")
                    cleanupGatt()
                }
            } else {
                log("BLE Service discovery failed with status $status")
                _connectionState.value = BluetoothConnectionState.Error("BLE service discovery failed")
            }
        }

        // Handles characteristic notification values (API < 33)
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handleReceivedBytes(value)
        }

        // Handles characteristic notification values (API 33+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleReceivedBytes(value)
        }
    }

    private fun handleReceivedBytes(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        _totalBytesReceived.value += bytes.size

        val parser = neuroSkyParser ?: NeuroSkyParser(currentConnectedDeviceType).also { neuroSkyParser = it }
        val telemetry = parser.parseBytes(bytes, bytes.size)
        _latestTelemetry.value = telemetry
    }

    fun hasPermissions(): Boolean {
        return hasScanPermission() && hasConnectPermission()
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasConnectPermission() || bluetoothAdapter == null) return

        try {
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            _pairedDevices.value = bonded.map { dev ->
                val devName = dev.name ?: "Unknown Device"
                EegDevice(
                    name = devName,
                    address = dev.address,
                    isPaired = true,
                    deviceType = EegDeviceType.detectType(devName)
                )
            }
            log("Loaded ${_pairedDevices.value.size} paired Bluetooth device(s).")
        } catch (e: Exception) {
            log("Error getting paired devices: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            log("Cannot start scan: permissions missing or Bluetooth disabled.")
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth disabled or permissions missing")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = BluetoothConnectionState.Scanning

        registerReceiver()

        // 1. Start Classic Bluetooth discovery
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        val classicStarted = bluetoothAdapter.startDiscovery()
        if (classicStarted) {
            log("Started Bluetooth classic discovery...")
        }

        // 2. Start BLE Scan for BrainLink Lite V2.0 / BLE Devices
        try {
            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            if (bleScanner != null) {
                bleScanner.startScan(bleScanCallback)
                isBleScanning = true
                log("Started BLE scanner...")
            }
        } catch (e: Exception) {
            log("BLE scan start notice: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (hasScanPermission() && bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        if (isBleScanning && hasScanPermission()) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            } catch (_: Exception) {}
            isBleScanning = false
        }
        if (_connectionState.value is BluetoothConnectionState.Scanning) {
            _connectionState.value = BluetoothConnectionState.Disconnected
        }
        unregisterReceiver()
        log("Stopped Bluetooth scan.")
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        if (!hasConnectPermission() || bluetoothAdapter == null) {
            _connectionState.value = BluetoothConnectionState.Error("Missing BLUETOOTH_CONNECT permission")
            return
        }

        stopScan()
        disconnect()

        val device = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } catch (_: Exception) {
            log("Invalid Bluetooth device address: $deviceAddress")
            _connectionState.value = BluetoothConnectionState.Error("Invalid device address")
            return
        }

        val deviceName = try { device.name ?: deviceAddress } catch (_: Exception) { deviceAddress }
        val devType = EegDeviceType.detectType(deviceName)
        currentConnectedDeviceType = devType
        neuroSkyParser = NeuroSkyParser(devType)

        _connectionState.value = BluetoothConnectionState.Connecting(deviceName)
        log("Initiating connection to ${devType.displayName}: $deviceName [$deviceAddress]...")

        // Direct BLE GATT Connection ONLY for BrainLink Lite
        if (devType == EegDeviceType.BRAINLINK_LITE) {
            log("Attempting direct BLE GATT connection for BrainLink...")
            connectBleGatt(device)
            return
        }

        // Standard SPP RFCOMM Connection
        connectionJob = scope.launch {
            try {
                // Cancel active scanning and wait 400ms for Bluetooth hardware radio to settle
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
                delay(400.milliseconds)

                // Auto-Bond check if not paired yet
                if (device.bondState == BluetoothDevice.BOND_NONE) {
                    log("Appareil $deviceName non appairé. Tentative d'appairage automatique (createBond)...")
                    try {
                        val bondStarted = device.createBond()
                        if (bondStarted) {
                            log("Demande d'appairage envoyée. Veuillez valider la popup Bluetooth sur le téléphone (PIN 0000).")
                            var waitCount = 0
                            while (device.bondState == BluetoothDevice.BOND_BONDING && waitCount < 15) {
                                delay(300.milliseconds)
                                waitCount++
                            }
                        }
                    } catch (eBond: Exception) {
                        log("Notice appairage: ${eBond.localizedMessage}")
                    }
                }

                var tempSocket: BluetoothSocket? = null
                var sppSuccess = false

                // Try 2 connection attempts (Attempt 1: instant, Attempt 2 after 500ms delay)
                for (attempt in 1..2) {
                    if (sppSuccess) break
                    if (attempt > 1) {
                        log("Re-tentative de connexion SPP (essai $attempt/2)...")
                        delay(500.milliseconds)
                    }

                    // Strategy 1: Reflection Channel 1 Direct (Evite le timeout SDP sur OnePlus / OxygenOS)
                    try {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        tempSocket = method.invoke(device, 1) as BluetoothSocket
                        tempSocket.connect()
                        sppSuccess = true
                        log("Connecté via canal RFCOMM 1 direct.")
                    } catch (_: Exception) {
                        try { tempSocket?.close() } catch (_: Exception) {}
                        tempSocket = null

                        // Strategy 2: Insecure SPP Socket
                        try {
                            tempSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                            tempSocket.connect()
                            sppSuccess = true
                            log("Connecté via socket SPP non sécuré.")
                        } catch (_: Exception) {
                            try { tempSocket?.close() } catch (_: Exception) {}
                            tempSocket = null

                            // Strategy 3: Standard Secure SPP Socket
                            try {
                                tempSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                                tempSocket.connect()
                                sppSuccess = true
                                log("Connecté via socket SPP standard.")
                            } catch (_: Exception) {
                                try { tempSocket?.close() } catch (_: Exception) {}
                                tempSocket = null

                                // Strategy 4: Reflection Channel 2
                                try {
                                    val method2 = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                                    tempSocket = method2.invoke(device, 2) as BluetoothSocket
                                    tempSocket.connect()
                                    sppSuccess = true
                                    log("Connecté via canal RFCOMM 2.")
                                } catch (_: Exception) {
                                    try { tempSocket?.close() } catch (_: Exception) {}
                                    tempSocket = null
                                }
                            }
                        }
                    }
                }

                if (sppSuccess && tempSocket != null) {
                    socket = tempSocket
                    inputStream = tempSocket.inputStream
                    outputStream = tempSocket.outputStream

                    _connectionState.value = BluetoothConnectionState.Connected(
                        deviceName = deviceName,
                        deviceAddress = deviceAddress,
                        connectionType = ConnectionType.SPP
                    )

                    log("Connecté en SPP à ${devType.displayName} ($deviceName) avec succès.")

                    if (devType == EegDeviceType.PLUX) {
                        startAcquisition()
                    }

                    startThroughputCalculator()
                    startListeningForData(devType)

                } else {
                    log("Échec des tentatives SPP pour $deviceName. Tentative de secours BLE GATT...")
                    withContext(Dispatchers.Main) {
                        connectBleGatt(device)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                log("Erreur de connexion SPP : ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.Error(
                        e.localizedMessage ?: "Échec de connexion à $deviceName"
                    )
                }
                cleanupSocket()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBleGatt(device: BluetoothDevice) {
        try {
            cleanupGatt()
            log("Connecting GATT to ${device.name ?: device.address}...")
            val gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            bluetoothGatt = gatt

            // Watchdog : si aucun callback GATT après 12 s, on abandonne proprement
            bleConnectTimeoutJob?.cancel()
            bleConnectTimeoutJob = scope.launch {
                delay(12_000)
                if (bluetoothGatt === gatt &&
                    _connectionState.value is BluetoothConnectionState.Connecting
                ) {
                    log("BLE GATT connection timed out after 12 s.")
                    cleanupGatt()
                    _connectionState.value = BluetoothConnectionState.Error(
                        "Délai de connexion BLE dépassé. L'appareil ne répond pas en Bluetooth Low Energy."
                    )
                }
            }
        } catch (e: Exception) {
            log("BLE GATT connection exception: ${e.localizedMessage}")
            _connectionState.value = BluetoothConnectionState.Error("BLE GATT error: ${e.localizedMessage}")
        }
    }

    private fun startListeningForData(deviceType: EegDeviceType) {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            var bytesRead: Int

            val parser = neuroSkyParser ?: NeuroSkyParser(deviceType).also { neuroSkyParser = it }

            log("Started reading byte stream from ${deviceType.displayName}...")

            while (isActive && socket?.isConnected == true) {
                try {
                    val stream = inputStream ?: break
                    bytesRead = stream.read(buffer)
                    if (bytesRead > 0) {
                        _totalBytesReceived.value += bytesRead

                        when (deviceType) {
                            EegDeviceType.PLUX -> {
                                val pluxFrame = PluxFrame.parse(buffer, bytesRead)
                                _latestTelemetry.value = pluxFrame.toTelemetry()
                            }
                            EegDeviceType.MINDWAVE_MOBILE,
                            EegDeviceType.BRAINLINK_LITE,
                            -> {
                                val telemetry = parser.parseBytes(buffer, bytesRead)
                                _latestTelemetry.value = telemetry
                            }
                            EegDeviceType.GENERIC -> {
                                val telemetry = parser.parseBytes(buffer, bytesRead)
                                if (telemetry.signalQuality < 200 || telemetry.attention > 0) {
                                    _latestTelemetry.value = telemetry
                                } else {
                                    val pluxFrame = PluxFrame.parse(buffer, bytesRead)
                                    _latestTelemetry.value = pluxFrame.toTelemetry().copy(deviceType = EegDeviceType.GENERIC)
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        log("Stream interrupted: ${e.localizedMessage}")
                        _connectionState.value = BluetoothConnectionState.Error("Connection lost")
                        cleanupSocket()
                    }
                    break
                }
            }
        }
    }

    private fun startThroughputCalculator() {
        throughputJob?.cancel()
        throughputJob = scope.launch {
            var previousTotal = _totalBytesReceived.value
            while (isActive && (_connectionState.value is BluetoothConnectionState.Connected)) {
                delay(1.seconds)
                val currentTotal = _totalBytesReceived.value
                val diff = (currentTotal - previousTotal).toInt()
                _bytesPerSecond.value = diff
                previousTotal = currentTotal
            }
        }
    }

    fun sendCommand(command: ByteArray): Boolean {
        return try {
            val stream = outputStream
            if (socket?.isConnected == true && stream != null) {
                stream.write(command)
                stream.flush()
                val hexStr = command.joinToString(" ") { String.format("%02X", it) }
                log("Sent command bytes to SPP device: [ $hexStr ]")
                true
            } else if (bluetoothGatt != null && notifyCharacteristic != null) {
                val hexStr = command.joinToString(" ") { String.format("%02X", it) }
                log("BLE command bytes ready: [ $hexStr ]")
                true
            } else {
                log("Cannot send command: Device not connected.")
                false
            }
        } catch (e: IOException) {
            log("Error sending command: ${e.localizedMessage}")
            false
        }
    }

    fun startAcquisition(): Boolean {
        return if (currentConnectedDeviceType == EegDeviceType.PLUX) {
            log("Sending START acquisition command (0x01)...")
            sendCommand(COMMAND_PLUX_START)
        } else {
            log("Device ${currentConnectedDeviceType.displayName} streams automatically once connected.")
            true
        }
    }

    fun stopAcquisition(): Boolean {
        return if (currentConnectedDeviceType == EegDeviceType.PLUX) {
            log("Sending STOP acquisition command (0x00)...")
            sendCommand(COMMAND_PLUX_STOP)
        } else {
            true
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        readJob?.cancel()
        throughputJob?.cancel()
        bleConnectTimeoutJob?.cancel()

        log("Disconnecting device...")
        if (currentConnectedDeviceType == EegDeviceType.PLUX) {
            try {
                outputStream?.write(COMMAND_PLUX_STOP)
                outputStream?.flush()
            } catch (_: Exception) {}
        }
        cleanupSocket()
        cleanupGatt()
        log("Disconnected.")

        _connectionState.value = BluetoothConnectionState.Disconnected
        _bytesPerSecond.value = 0
    }

    private fun cleanupSocket() {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    @SuppressLint("MissingPermission")
    private fun cleanupGatt() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
        notifyCharacteristic = null
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
            isReceiverRegistered = false
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        _logEvents.tryEmit(message)
    }
}
