package com.example.mybrainlive.bluetooth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybrainlive.audio.MeditationTonePlayer
import com.example.mybrainlive.export.EegExporter
import com.example.mybrainlive.export.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque

data class EegUiState(
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.Disconnected,
    val pairedDevices: List<EegDevice> = emptyList(),
    val scannedDevices: List<EegDevice> = emptyList(),
    val latestTelemetry: EegTelemetry? = null,
    val totalBytesReceived: Long = 0L,
    val bytesPerSecond: Int = 0,
    val isPermissionGranted: Boolean = false,
    val isStreaming: Boolean = false,
    val logs: List<String> = emptyList(),
    val selectedFilter: EegDeviceType? = null,
    val expandedDeviceAddress: String? = null,
    val telemetryHistory: List<EegTelemetry> = emptyList(),
    val rawWaveHistory: List<Int> = emptyList(),
    val eSenseHistory: List<EegTelemetry> = emptyList(),
    val isRecording: Boolean = false,
    val recordingStartTime: Long = 0L,
    val recordedSamples: List<EegTelemetry> = emptyList(),
    val showExportDialog: Boolean = false,
    val lastExportedFile: File? = null,
    val lastExportedFormat: ExportFormat? = null,
    // Retour sonore de méditation
    val audioFeedbackEnabled: Boolean = false,
    val meditationSampleThreshold: Int = 60,   // eSense >= seuil => échantillon "méditatif"
    val meditationPercentThreshold: Int = 70,  // % requis sur la fenêtre pour déclencher le son
    val meditationWindowPercent: Float = 0f,   // % actuel d'échantillons méditatifs (30 dernières s)
    val isMeditatingState: Boolean = false,
) {
    val filteredPairedDevices: List<EegDevice>
        get() = if (selectedFilter == null) pairedDevices else pairedDevices.filter { it.deviceType == selectedFilter }

    val filteredScannedDevices: List<EegDevice>
        get() = if (selectedFilter == null) scannedDevices else scannedDevices.filter { it.deviceType == selectedFilter }

    val latestFrame: PluxFrame?
        get() = latestTelemetry?.let {
            PluxFrame(
                sequence = it.sequenceNumber,
                digitalInputs = emptyList(),
                analogChannels = it.pluxChannels,
                rawBytesHex = it.rawBytesHex,
                timestamp = it.timestamp,
            )
        }
}

class EegViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothManager = EegBluetoothManager(application.applicationContext)

    private val _uiState = MutableStateFlow(EegUiState())
    val uiState: StateFlow<EegUiState> = _uiState.asStateFlow()

    // Fenêtre glissante des échantillons de méditation (timestamp ms -> valeur eSense)
    private val meditationWindow = ArrayDeque<Pair<Long, Int>>()
    private var lastChimeTime = 0L

    companion object {
        const val MEDITATION_WINDOW_MS = 30_000L      // fenêtre des 30 dernières secondes
        const val CHIME_REPEAT_INTERVAL_MS = 10_000L  // rappel sonore si l'état persiste
    }

    init {
        checkPermissions()
        observeManagerFlows()
    }

    fun checkPermissions() {
        val granted = bluetoothManager.hasPermissions()
        _uiState.value = _uiState.value.copy(isPermissionGranted = granted)
        if (granted) {
            bluetoothManager.refreshPairedDevices()
        }
    }

    fun onPermissionsResult(allGranted: Boolean) {
        _uiState.value = _uiState.value.copy(isPermissionGranted = allGranted)
        if (allGranted) {
            bluetoothManager.refreshPairedDevices()
            addLog("Bluetooth permissions granted.")
        } else {
            addLog("Bluetooth permissions were denied.")
        }
    }

    private fun observeManagerFlows() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        viewModelScope.launch {
            bluetoothManager.pairedDevices.collect { devices ->
                _uiState.value = _uiState.value.copy(pairedDevices = devices)
            }
        }

        viewModelScope.launch {
            bluetoothManager.scannedDevices.collect { devices ->
                _uiState.value = _uiState.value.copy(scannedDevices = devices)
            }
        }

        viewModelScope.launch {
            bluetoothManager.latestTelemetry.collect { telemetry ->
                val history = _uiState.value.telemetryHistory.toMutableList()
                val rawHistory = _uiState.value.rawWaveHistory.toMutableList()
                val eSenseList = _uiState.value.eSenseHistory.toMutableList()
                val recorded = _uiState.value.recordedSamples.toMutableList()

                if (telemetry != null) {
                    history.add(telemetry)
                    if (history.size > 120) {
                        history.removeAt(0)
                    }

                    // Raw Wave Oscilloscope history
                    rawHistory.add(telemetry.rawWave)
                    if (rawHistory.size > 150) {
                        rawHistory.removeAt(0)
                    }

                    // eSense Attention & Meditation timeline history (sampled ~every 800ms)
                    val lastTime = eSenseList.lastOrNull()?.timestamp ?: 0L
                    if ((telemetry.timestamp - lastTime >= 800L) || eSenseList.isEmpty()) {
                        eSenseList.add(telemetry)
                        if (eSenseList.size > 120) {
                            eSenseList.removeAt(0)
                        }
                    }

                    if (_uiState.value.isRecording) {
                        recorded.add(telemetry)
                    }
                }
                val meditationFeedback = updateMeditationFeedback(telemetry)
                _uiState.value = _uiState.value.copy(
                    latestTelemetry = telemetry,
                    telemetryHistory = history,
                    rawWaveHistory = rawHistory,
                    eSenseHistory = eSenseList,
                    recordedSamples = recorded,
                    meditationWindowPercent = meditationFeedback.first,
                    isMeditatingState = meditationFeedback.second,
                )
            }
        }

        viewModelScope.launch {
            bluetoothManager.totalBytesReceived.collect { total ->
                _uiState.value = _uiState.value.copy(totalBytesReceived = total)
            }
        }

        viewModelScope.launch {
            bluetoothManager.bytesPerSecond.collect { rate ->
                _uiState.value = _uiState.value.copy(bytesPerSecond = rate)
            }
        }

        viewModelScope.launch {
            bluetoothManager.logEvents.collect { logMsg ->
                addLog(logMsg)
            }
        }
    }

    /**
     * Met à jour la fenêtre glissante des 30 dernières secondes et détermine
     * si le pourcentage d'échantillons "méditatifs" dépasse le seuil configuré.
     * Retourne (pourcentage actuel, état méditatif actif).
     */
    private fun updateMeditationFeedback(telemetry: EegTelemetry?): Pair<Float, Boolean> {
        val state = _uiState.value
        val now = System.currentTimeMillis()

        // Seuls les casques type NeuroSky fournissent un eSense méditation fiable
        val isNeuroSkyDevice = telemetry?.deviceType == EegDeviceType.MINDWAVE_MOBILE ||
                telemetry?.deviceType == EegDeviceType.BRAINLINK_LITE

        if (telemetry != null && isNeuroSkyDevice && telemetry.isHeadsetConnected) {
            meditationWindow.addLast(telemetry.timestamp to telemetry.meditation)
        }

        // Purge des échantillons plus vieux que la fenêtre
        while (meditationWindow.isNotEmpty() && now - meditationWindow.peekFirst()!!.first > MEDITATION_WINDOW_MS) {
            meditationWindow.removeFirst()
        }

        if (meditationWindow.isEmpty()) {
            return 0f to false
        }

        val meditativeCount = meditationWindow.count { it.second >= state.meditationSampleThreshold }
        val percent = (meditativeCount * 100f) / meditationWindow.size
        val isMeditating = percent >= state.meditationPercentThreshold

        // Déclenchement du retour sonore : à l'entrée dans l'état, puis rappel périodique
        if (state.audioFeedbackEnabled && isMeditating) {
            val wasMeditating = state.isMeditatingState
            if (!wasMeditating || now - lastChimeTime >= CHIME_REPEAT_INTERVAL_MS) {
                MeditationTonePlayer.playChime()
                lastChimeTime = now
                if (!wasMeditating) {
                    addLog("État de méditation détecté (${percent.toInt()}% sur 30 s) — retour sonore.")
                }
            }
        }

        return percent to isMeditating
    }

    fun toggleAudioFeedback() {
        val enabled = !_uiState.value.audioFeedbackEnabled
        if (!enabled) {
            meditationWindow.clear()
        }
        _uiState.value = _uiState.value.copy(audioFeedbackEnabled = enabled)
        addLog(if (enabled) "Retour sonore de méditation activé." else "Retour sonore de méditation désactivé.")
    }

    fun setMeditationSampleThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(meditationSampleThreshold = threshold.coerceIn(1, 100))
    }

    fun setMeditationPercentThreshold(percent: Int) {
        _uiState.value = _uiState.value.copy(meditationPercentThreshold = percent.coerceIn(1, 100))
    }

    fun toggleDeviceExpanded(deviceAddress: String) {
        val current = _uiState.value.expandedDeviceAddress
        _uiState.value = _uiState.value.copy(
            expandedDeviceAddress = if (current == deviceAddress) null else deviceAddress
        )
    }

    fun setDeviceFilter(filter: EegDeviceType?) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun startScan() {
        bluetoothManager.startScan()
    }

    fun stopScan() {
        bluetoothManager.stopScan()
    }

    fun refreshPairedDevices() {
        bluetoothManager.refreshPairedDevices()
    }

    fun connect(deviceAddress: String) {
        _uiState.value = _uiState.value.copy(expandedDeviceAddress = deviceAddress)
        bluetoothManager.connectToDevice(deviceAddress)
    }

    fun disconnect() {
        bluetoothManager.disconnect()
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    fun startAcquisition() {
        val success = bluetoothManager.startAcquisition()
        if (success) {
            _uiState.value = _uiState.value.copy(isStreaming = true)
        }
    }

    fun stopAcquisition() {
        val success = bluetoothManager.stopAcquisition()
        if (success) {
            _uiState.value = _uiState.value.copy(isStreaming = false)
        }
    }

    fun sendHexCommand(hexString: String) {
        val cleanHex = hexString.replace(" ", "")
        if ((cleanHex.length % 2) != 0) {
            addLog("Invalid hex string length.")
            return
        }
        val bytes = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        bluetoothManager.sendCommand(bytes)
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }

    fun startRecording() {
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingStartTime = System.currentTimeMillis(),
            recordedSamples = emptyList(),
            lastExportedFile = null,
            lastExportedFormat = null
        )
        addLog("Démarrage de l'enregistrement de la session EEG...")
    }

    fun stopRecording() {
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            showExportDialog = true
        )
        addLog("Fin de l'enregistrement (${_uiState.value.recordedSamples.size} échantillons capturés).")
    }

    fun exportSession(context: Context, format: ExportFormat) {
        val listToExport = if (_uiState.value.recordedSamples.isNotEmpty()) {
            _uiState.value.recordedSamples
        } else if (_uiState.value.telemetryHistory.isNotEmpty()) {
            _uiState.value.telemetryHistory
        } else if (_uiState.value.latestTelemetry != null) {
            listOfNotNull(_uiState.value.latestTelemetry)
        } else {
            emptyList()
        }

        val file = EegExporter.exportSession(
            context = context,
            telemetryList = listToExport,
            format = format
        )
        if (file != null) {
            _uiState.value = _uiState.value.copy(
                lastExportedFile = file,
                lastExportedFormat = format
            )
            addLog("Fichier ${format.extension.uppercase()} exporté (${listToExport.size} échantillons) : ${file.name}")
        } else {
            addLog("Échec de l'exportation du fichier.")
        }
    }

    fun shareExportedFile(context: Context) {
        val file = _uiState.value.lastExportedFile
        val format = _uiState.value.lastExportedFormat
        if ((file != null) && (format != null)) {
            EegExporter.shareFile(context, file, format.mimeType)
        }
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    private fun addLog(message: String) {
        val current = _uiState.value.logs.toMutableList()
        current.add(0, message)
        if (current.size > 100) {
            current.removeAt(current.lastIndex)
        }
        _uiState.value = _uiState.value.copy(logs = current)
    }
}
