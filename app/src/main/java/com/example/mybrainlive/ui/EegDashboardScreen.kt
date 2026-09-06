package com.example.mybrainlive.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybrainlive.bluetooth.BluetoothConnectionState
import com.example.mybrainlive.bluetooth.EegDevice
import com.example.mybrainlive.bluetooth.EegDeviceType
import com.example.mybrainlive.bluetooth.EegTelemetry
import com.example.mybrainlive.bluetooth.EegUiState
import com.example.mybrainlive.bluetooth.EegViewModel
import com.example.mybrainlive.export.ExportFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EegDashboardScreen(viewModel: EegViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionsResult(allGranted)
    }

    var activeScreenTab by remember { mutableIntStateOf(0) } // 0 = Connexion BT, 1 = Ondes EEG, 2 = Graphiques

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "My Brain Live",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (activeScreenTab) {
                                    0 -> "Connexion Casques & Capteurs BT"
                                    1 -> "Spectre & Métriques EEG"
                                    else -> "Visualisation Graphique Temporel"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    ConnectionStatusBadge(connectionState = uiState.connectionState)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeScreenTab == 0,
                    onClick = { activeScreenTab = 0 },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                    label = { Text("Connexion BT") }
                )
                NavigationBarItem(
                    selected = activeScreenTab == 1,
                    onClick = { activeScreenTab = 1 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("Ondes EEG") }
                )
                NavigationBarItem(
                    selected = activeScreenTab == 2,
                    onClick = { activeScreenTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
                    label = { Text("Graphiques") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Export Dialog if requested
            if (uiState.showExportDialog) {
                EegExportDialog(uiState = uiState, viewModel = viewModel)
            }
            // Permission Banner if missing
            AnimatedVisibility(visible = !uiState.isPermissionGranted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autorisations Bluetooth requises",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Permet de rechercher et se connecter aux casques EEG Bluetooth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                } else {
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                }
                                permissionLauncher.launch(permissions)
                            }
                        ) {
                            Text("Accorder")
                        }
                    }
                }
            }

            when (activeScreenTab) {
                0 -> {
                    // TAB 0: PAGE DÉDIÉE À LA CONNEXION BT
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Filter Chips & Scan Controls
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recherche Appareils",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.refreshPairedDevices() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                                        }

                                        if (uiState.connectionState is BluetoothConnectionState.Scanning) {
                                            Button(
                                                onClick = { viewModel.stopScan() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onSecondary,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Stop")
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.startScan() },
                                                enabled = uiState.isPermissionGranted
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scanner")
                                            }
                                        }
                                    }
                                }

                                // Filter chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    FilterChip(
                                        selected = uiState.selectedFilter == null,
                                        onClick = { viewModel.setDeviceFilter(null) },
                                        label = { Text("Tous") }
                                    )
                                    FilterChip(
                                        selected = uiState.selectedFilter == EegDeviceType.PLUX,
                                        onClick = { viewModel.setDeviceFilter(EegDeviceType.PLUX) },
                                        label = { Text("PLUX") }
                                    )
                                    FilterChip(
                                        selected = uiState.selectedFilter == EegDeviceType.MINDWAVE_MOBILE,
                                        onClick = { viewModel.setDeviceFilter(EegDeviceType.MINDWAVE_MOBILE) },
                                        label = { Text("MindWave") }
                                    )
                                    FilterChip(
                                        selected = uiState.selectedFilter == EegDeviceType.BRAINLINK_LITE,
                                        onClick = { viewModel.setDeviceFilter(EegDeviceType.BRAINLINK_LITE) },
                                        label = { Text("BrainLink") }
                                    )
                                }
                            }
                        }

                        // Devices List with expandable inline log encart
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val pairedList = uiState.filteredPairedDevices
                            val scannedList = uiState.filteredScannedDevices

                            if (pairedList.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Casques Appairés (${pairedList.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(pairedList) { device ->
                                    ExpandableDeviceCard(
                                        device = device,
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            if (scannedList.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Appareils Découverts à Proximité (${scannedList.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                                items(scannedList) { device ->
                                    ExpandableDeviceCard(
                                        device = device,
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            if (pairedList.isEmpty() && scannedList.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Aucun casque EEG trouvé. Allumez votre MindWave 2, BrainLink Lite V2.0 ou PLUX et touchez 'Scanner'.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: PAGE TÉLÉMESURE & METRIQUES EEG
                    val telemetry = uiState.latestTelemetry
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EegRecordingBar(uiState = uiState, viewModel = viewModel)

                        if (telemetry != null) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ondes Cérébrales en Direct",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        DeviceTypeBadge(deviceType = telemetry.deviceType)
                                    }

                                    if ((telemetry.deviceType == EegDeviceType.MINDWAVE_MOBILE) || (telemetry.deviceType == EegDeviceType.BRAINLINK_LITE)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            ESenseMetricBox(
                                                title = "Attention (Focus)",
                                                value = telemetry.attention,
                                                color = Color(0xFF1976D2)
                                            )
                                            ESenseMetricBox(
                                                title = "Méditation (Calme)",
                                                value = telemetry.meditation,
                                                color = Color(0xFF7B1FA2)
                                            )
                                        }

                                        EegBandRow("Delta (0.5-2.75 Hz - Sommeil/Inconscient)", telemetry.eegBands.delta, telemetry.eegBands.totalPower, Color(0xFF311B92))
                                        EegBandRow("Theta (3.5-6.75 Hz - Rêve/Créativité)", telemetry.eegBands.theta, telemetry.eegBands.totalPower, Color(0xFF4A148C))
                                        EegBandRow("Low Alpha (7.5-9.25 Hz - Relaxation)", telemetry.eegBands.lowAlpha, telemetry.eegBands.totalPower, Color(0xFF006064))
                                        EegBandRow("High Alpha (10-11.75 Hz - Calme Lucide)", telemetry.eegBands.highAlpha, telemetry.eegBands.totalPower, Color(0xFF004D40))
                                        EegBandRow("Low Beta (13-16.75 Hz - Reflexion)", telemetry.eegBands.lowBeta, telemetry.eegBands.totalPower, Color(0xFF1B5E20))
                                        EegBandRow("High Beta (18-29.75 Hz - Focus/Anxiété)", telemetry.eegBands.highBeta, telemetry.eegBands.totalPower, Color(0xFFE65100))
                                        EegBandRow("Low Gamma (31-39.75 Hz - Cognition)", telemetry.eegBands.lowGamma, telemetry.eegBands.totalPower, Color(0xFFBF360C))
                                        EegBandRow("Mid Gamma (41-49.75 Hz - Multi-Reflexe)", telemetry.eegBands.midGamma, telemetry.eegBands.totalPower, Color(0xFF880E4F))

                                    } else if (telemetry.deviceType == EegDeviceType.PLUX) {
                                        Text("Canaux Analogiques PLUX:", fontWeight = FontWeight.Bold)
                                        telemetry.pluxChannels.forEachIndexed { idx, val16 ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Canal A${idx + 1}")
                                                Text("$val16 raw", fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aucune donnée de télémesure active. Rendez-vous dans 'Connexion BT' pour sélectionner votre casque.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: PAGE DÉDIÉE VISUALISATION GRAPHIQUE DES ONDES
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EegRecordingBar(uiState = uiState, viewModel = viewModel)
                        }

                        item {
                            // Graphique 1: Oscilloscope Onde Brute EEG
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Oscilloscope Onde Brute (µV)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.width(100.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%+4d µV", uiState.latestTelemetry?.rawWave ?: 0),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF38BDF8),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    RawWaveformChart(history = uiState.telemetryHistory)
                                }
                            }
                        }

                        item {
                            // Graphique 2: Courbes d'Attention & Méditation
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Évolution Temporelle Attention & Méditation",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF1976D2)))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = String.format(Locale.US, "Attention: %3d/100", uiState.latestTelemetry?.attention ?: 0),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF9C27B0)))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = String.format(Locale.US, "Méditation: %3d/100", uiState.latestTelemetry?.meditation ?: 0),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    AttentionMeditationChart(history = uiState.telemetryHistory)
                                }
                            }
                        }

                        item {
                            // Graphique 3: Analyseur du Spectre Cérébral (Bar Chart)
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Analyseur du Spectre Fréquentiel (ASIC Power)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    BrainwaveSpectrumChart(telemetry = uiState.latestTelemetry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RawWaveformChart(history: List<EegTelemetry>) {
    val points = history.map { it.rawWave.toFloat() }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Draw oscilloscope grid lines
            val gridColor = Color(0xFF1E293B)
            drawLine(gridColor, Offset(0f, midY), Offset(width, midY), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1f)

            if (points.size >= 2) {
                val minVal = (points.minOrNull() ?: -100f).coerceAtMost(-10f)
                val maxVal = (points.maxOrNull() ?: 100f).coerceAtLeast(10f)
                val range = (maxVal - minVal).coerceAtLeast(1f)

                val dx = width / (points.size - 1).coerceAtLeast(1)
                val path = Path()

                points.forEachIndexed { i, raw ->
                    val x = i * dx
                    val normY = (raw - minVal) / range
                    val y = height - (normY * height)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF38BDF8),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun AttentionMeditationChart(history: List<EegTelemetry>) {
    val attPoints = history.map { it.attention.toFloat().coerceIn(0f, 100f) }
    val medPoints = history.map { it.meditation.toFloat().coerceIn(0f, 100f) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val width = size.width
            val height = size.height

            // Guide lines (25%, 50%, 75%)
            val gridColor = Color(0xFF1E293B)
            listOf(0.25f, 0.5f, 0.75f).forEach { pct ->
                drawLine(gridColor, Offset(0f, height * (1 - pct)), Offset(width, height * (1 - pct)), strokeWidth = 1f)
            }

            // Draw Attention Line (Blue)
            if (attPoints.size >= 2) {
                val dx = width / (attPoints.size - 1).coerceAtLeast(1)
                val attPath = Path()
                attPoints.forEachIndexed { i, att ->
                    val x = i * dx
                    val y = height - ((att / 100f) * height)
                    if (i == 0) attPath.moveTo(x, y) else attPath.lineTo(x, y)
                }
                drawPath(
                    path = attPath,
                    color = Color(0xFF1976D2),
                    style = Stroke(width = 5f)
                )
            }

            // Draw Meditation Line (Purple)
            if (medPoints.size >= 2) {
                val dx = width / (medPoints.size - 1).coerceAtLeast(1)
                val medPath = Path()
                medPoints.forEachIndexed { i, med ->
                    val x = i * dx
                    val y = height - ((med / 100f) * height)
                    if (i == 0) medPath.moveTo(x, y) else medPath.lineTo(x, y)
                }
                drawPath(
                    path = medPath,
                    color = Color(0xFF9C27B0),
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}

@Composable
fun BrainwaveSpectrumChart(telemetry: EegTelemetry?) {
    val bands = telemetry?.eegBands
    val total = bands?.totalPower ?: 1

    val items = listOf(
        Triple("Delta", bands?.delta ?: 0, Color(0xFF311B92)),
        Triple("Theta", bands?.theta ?: 0, Color(0xFF4A148C)),
        Triple("L-Alpha", bands?.lowAlpha ?: 0, Color(0xFF006064)),
        Triple("H-Alpha", bands?.highAlpha ?: 0, Color(0xFF004D40)),
        Triple("L-Beta", bands?.lowBeta ?: 0, Color(0xFF1B5E20)),
        Triple("H-Beta", bands?.highBeta ?: 0, Color(0xFFE65100)),
        Triple("L-Gamma", bands?.lowGamma ?: 0, Color(0xFFBF360C)),
        Triple("M-Gamma", bands?.midGamma ?: 0, Color(0xFF880E4F))
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { (_, valInt, color) ->
                    val pct = if (total > 0) (valInt.toFloat() / total.toFloat()).coerceIn(0.05f, 1f) else 0.05f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxSize(fraction = pct)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { (name, _, _) ->
                    Text(
                        text = name,
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableDeviceCard(
    device: EegDevice,
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    val isExpanded = uiState.expandedDeviceAddress == device.address
    val isConnected = uiState.connectionState is BluetoothConnectionState.Connected &&
            uiState.connectionState.deviceAddress == device.address
    val isConnecting = uiState.connectionState is BluetoothConnectionState.Connecting &&
            uiState.connectionState.deviceName == device.name

    var customHexCommand by remember { mutableStateOf("01") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isConnected) 2.dp else 1.dp,
                color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleDeviceExpanded(device.address) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            DeviceTypeBadge(deviceType = device.deviceType)
                        }
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (isConnected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E7D32)
                        ) {
                            Text(
                                text = "CONNECTÉ",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ouvrir encart logs et actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isConnected) {
                            Button(
                                onClick = { viewModel.startAcquisition() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Démarrer (0x01)", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.stopAcquisition() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Arrêter (0x00)", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.disconnect() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Déconnecter", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.connect(device.address) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecter à ${device.name}")
                            }
                        }
                    }

                    if (isConnected && uiState.latestTelemetry != null) {
                        val telemetry = uiState.latestTelemetry
                        if (device.deviceType == EegDeviceType.MINDWAVE_MOBILE || device.deviceType == EegDeviceType.BRAINLINK_LITE) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Contact Capteur:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${telemetry.signalFitPercent}% (${if (telemetry.isHeadsetConnected) "Pression OK" else "Ajuster"})", fontSize = 12.sp)
                                }
                                LinearProgressIndicator(
                                    progress = { telemetry.signalFitPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (telemetry.signalFitPercent >= 50) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Console de Logs - ${device.name}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearLogs() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer les logs", modifier = Modifier.size(16.dp))
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(Color(0xFF0F172A))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (uiState.logs.isEmpty()) {
                                item {
                                    Text(
                                        text = "> En attente d'événements de connexion ou de données...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                items(uiState.logs) { log ->
                                    Text(
                                        text = "> $log",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }

                    if (isConnected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customHexCommand,
                                onValueChange = { customHexCommand = it },
                                label = { Text("Commande Hex (ex: 01)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = { viewModel.sendHexCommand(customHexCommand) }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ESenseMetricBox(title: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (value > 0) "$value / 100" else "--",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EegBandRow(label: String, value: Int, total: Int, color: Color) {
    val pct = if (total > 0) (value.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
            Text(
                text = "$value (${(pct * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color
        )
    }
}

@Composable
fun DeviceTypeBadge(deviceType: EegDeviceType) {
    val (bgColor, textColor, label) = when (deviceType) {
        EegDeviceType.PLUX -> Triple(Color(0xFF00838F), Color.White, "PLUX")
        EegDeviceType.MINDWAVE_MOBILE -> Triple(Color(0xFF6A1B9A), Color.White, "MindWave 2")
        EegDeviceType.BRAINLINK_LITE -> Triple(Color(0xFFE65100), Color.White, "BrainLink")
        EegDeviceType.GENERIC -> Triple(Color(0xFF546E7A), Color.White, "SPP")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ConnectionStatusBadge(connectionState: BluetoothConnectionState) {
    val (color, text, icon) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Triple(Color(0xFF2E7D32), connectionState.deviceName, Icons.Default.BluetoothConnected)
        is BluetoothConnectionState.Connecting -> Triple(Color(0xFFF57C00), "Connexion...", Icons.Default.Bluetooth)
        is BluetoothConnectionState.Scanning -> Triple(Color(0xFF0288D1), "Recherche...", Icons.AutoMirrored.Filled.BluetoothSearching)
        is BluetoothConnectionState.Error -> Triple(MaterialTheme.colorScheme.error, "Erreur", Icons.Default.Warning)
        is BluetoothConnectionState.Disconnected -> Triple(Color.Gray, "Déconnecté", Icons.Default.Bluetooth)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun EegRecordingBar(
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isRecording) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = if (uiState.isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (uiState.isRecording) "ENREGISTREMENT EN COURS" else "Enregistrement Session EEG",
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isRecording) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (uiState.isRecording)
                            "${uiState.recordedSamples.size} échantillons capturés"
                        else
                            "Format HDF5 (.h5) ou Données Brutes (.csv)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.isRecording) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.isRecording) {
                Button(
                    onClick = { viewModel.stopRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Arrêter & Exporter", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.startRecording() },
                    enabled = uiState.connectionState is BluetoothConnectionState.Connected
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enregistrer", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EegExportDialog(
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.HDF5) }

    AlertDialog(
        onDismissRequest = { viewModel.dismissExportDialog() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sauvegarde / Exportation EEG", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Session enregistrée : ${uiState.recordedSamples.size} échantillons capturés.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choisissez le format de sauvegarde souhaité :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExportFormat.entries.forEach { format ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedFormat == format)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFormat = format }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = format.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = when (format) {
                                            ExportFormat.HDF5 -> "Conteneur scientifique .h5 compatible MNE / EEGLAB / HDFView"
                                            ExportFormat.CSV_RAW -> "Horodaté, lisible sous Excel, Python Pandas, MATLAB"
                                            ExportFormat.JSON -> "Format structuré Web & JSON"
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.lastExportedFile != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✅ Fichier Sauvegardé :",
                                color = Color(0xFF4ADE80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = uiState.lastExportedFile?.name ?: "",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Stocké dans Téléchargements/MyBrainLive",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.lastExportedFile != null) {
                    Button(
                        onClick = { viewModel.shareExportedFile(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Partager")
                    }
                }

                Button(
                    onClick = { viewModel.exportSession(context, selectedFormat) }
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.lastExportedFile == null) "Sauvegarder" else "Ré-exporter")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { viewModel.dismissExportDialog() }
            ) {
                Text("Fermer")
            }
        }
    )
}
