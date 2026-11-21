package com.example.mybrainlive.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybrainlive.audio.MeditationTonePlayer
import com.example.mybrainlive.bluetooth.AudioBiofeedbackMode
import com.example.mybrainlive.bluetooth.BluetoothConnectionState
import com.example.mybrainlive.bluetooth.EegDeviceType
import com.example.mybrainlive.bluetooth.EegTelemetry
import com.example.mybrainlive.bluetooth.EegUiState
import com.example.mybrainlive.bluetooth.EegViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class NeurofeedbackTarget(val label: String, val shortName: String) {
    MEDITATION("🧘 Méditation (Calme & Sérénité)", "Méditation"),
    CONCENTRATION("🎯 Concentration (Focus & Attention)", "Concentration")
}

data class BrainNodePosition(
    val name: String,
    val description: String,
    val normX: Float,
    val normY: Float
)

object Standard1020Nodes {
    val NODES = listOf(
        BrainNodePosition("FP1", "Frontal Pole Gauche", 0.36f, 0.21f),
        BrainNodePosition("FPz", "Frontal Pole Médian", 0.50f, 0.17f),
        BrainNodePosition("FP2", "Frontal Pole Droit", 0.64f, 0.21f),
        BrainNodePosition("F3", "Frontal Gauche", 0.32f, 0.35f),
        BrainNodePosition("Fz", "Frontal Médian (Focus)", 0.50f, 0.35f),
        BrainNodePosition("F4", "Frontal Droit", 0.68f, 0.35f),
        BrainNodePosition("C3", "Central Gauche", 0.28f, 0.50f),
        BrainNodePosition("Cz", "Sommet Cérébral", 0.50f, 0.50f),
        BrainNodePosition("C4", "Central Droit", 0.72f, 0.50f),
        BrainNodePosition("P3", "Pariétal Gauche", 0.34f, 0.65f),
        BrainNodePosition("Pz", "Pariétal Médian", 0.50f, 0.65f),
        BrainNodePosition("P4", "Pariétal Droit", 0.66f, 0.65f),
        BrainNodePosition("O1", "Occipital Gauche (Alpha)", 0.38f, 0.80f),
        BrainNodePosition("O2", "Occipital Droit (Alpha)", 0.62f, 0.80f),
        BrainNodePosition("A1", "Oreille Gauche", 0.12f, 0.50f),
        BrainNodePosition("A2", "Oreille Droite", 0.88f, 0.50f)
    )
}

@Composable
fun InteractiveBrainMapCard(
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    var selectedNodeForPlux by remember { mutableStateOf("FP1") }
    var isComparisonMode by remember { mutableStateOf(false) }

    val telemetry = uiState.latestTelemetry
    val connectedDeviceType = (uiState.connectionState as? BluetoothConnectionState.Connected)?.let {
        EegDeviceType.detectType(it.deviceName)
    }
    val currentDeviceType = telemetry?.deviceType
        ?: connectedDeviceType
        ?: uiState.selectedFilter
        ?: uiState.pairedDevices.firstOrNull { it.deviceType == EegDeviceType.BRAINLINK_LITE }?.deviceType
        ?: uiState.scannedDevices.firstOrNull { it.deviceType == EegDeviceType.BRAINLINK_LITE }?.deviceType
        ?: EegDeviceType.BRAINLINK_LITE
    val target = uiState.neurofeedbackTarget
    val score = when (target) {
        NeurofeedbackTarget.MEDITATION -> telemetry?.meditation ?: 0
        NeurofeedbackTarget.CONCENTRATION -> telemetry?.attention ?: 0
    }

    val baselineScore = if (uiState.eSenseHistory.isNotEmpty()) {
        when (target) {
            NeurofeedbackTarget.MEDITATION -> uiState.eSenseHistory.first().meditation
            NeurofeedbackTarget.CONCENTRATION -> uiState.eSenseHistory.first().attention
        }
    } else 35

    val activeColor = when (target) {
        NeurofeedbackTarget.CONCENTRATION -> Color(0xFFFFB300)
        NeurofeedbackTarget.MEDITATION -> Color(0xFF10B981)
    }

    val deltaPct = score - baselineScore

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cartographie Topographique Cérébrale",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                EducationalHelpButton(topic = EducationalTopics.RAW_WAVE)
            }

            // Target Selector & Comparison Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeurofeedbackTarget.entries.forEach { t ->
                    val isSelected = uiState.neurofeedbackTarget == t
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setNeurofeedbackTarget(t) },
                        label = {
                            Text(
                                text = t.shortName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                FilterChip(
                    selected = isComparisonMode,
                    onClick = { isComparisonMode = !isComparisonMode },
                    label = {
                        Text(
                            text = if (isComparisonMode) "Comparatif 📊" else "Vue Unique 🧠",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            // Live metrics bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${target.label} : $score / 100",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = activeColor
                )

                if (isComparisonMode) {
                    val sign = if (deltaPct >= 0) "+" else ""
                    val deltaColor = if (deltaPct >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    Text(
                        text = "Évolution : $sign$deltaPct%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = deltaColor
                    )
                } else {
                    Text(
                        text = if (telemetry?.isHeadsetConnected == true) "Signal Actif" else "Mode Démo",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = activeColor
            )

            // Topographical Heatmap Brain View
            if (isComparisonMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Avant / Repos ($baselineScore%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TopographicalBrainCanvas(
                            deviceType = currentDeviceType,
                            target = target,
                            score = baselineScore,
                            selectedNode = selectedNodeForPlux,
                            heightDp = 180,
                            onNodeSelected = { selectedNodeForPlux = it }
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "En direct / Actuel ($score%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TopographicalBrainCanvas(
                            deviceType = currentDeviceType,
                            target = target,
                            score = score,
                            selectedNode = selectedNodeForPlux,
                            heightDp = 180,
                            onNodeSelected = { selectedNodeForPlux = it }
                        )
                    }
                }
            } else {
                TopographicalBrainCanvas(
                    deviceType = currentDeviceType,
                    target = target,
                    score = score,
                    selectedNode = selectedNodeForPlux,
                    heightDp = 220,
                    onNodeSelected = { selectedNodeForPlux = it }
                )
            }

            // Information & Placement Guide
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = when (currentDeviceType) {
                            EegDeviceType.BRAINLINK_LITE ->
                                "📍 Emplacement Fixe (BrainLink Lite) : 3 capteurs frontaux sur le bandeau — FP1 (EEG actif), FPz (Masse / GND) et FP2 (Référence), sans clip d'oreille."
                            EegDeviceType.MINDWAVE_MOBILE ->
                                "📍 Emplacement Fixe (MindWave) : Électrode frontale en FP1 (Front Gauche) + Clip de référence oreille A1."
                            EegDeviceType.GENERIC ->
                                "📍 Emplacement Fixe (Capteur Générique SPP) : Électrode fronto-médiane en FPz (Front Médian) + Clip de référence oreille A1."
                            EegDeviceType.PLUX ->
                                "📍 Emplacement Libre (BITalino / PLUX) : Touchez un nœud sur la carte pour repositionner le canal ($selectedNodeForPlux)."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TopographicalBrainCanvas(
    deviceType: EegDeviceType,
    target: NeurofeedbackTarget,
    score: Int,
    selectedNode: String,
    heightDp: Int = 220,
    onNodeSelected: (String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B132B))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val closest = Standard1020Nodes.NODES.minByOrNull { node ->
                            val nx = node.normX * w
                            val ny = node.normY * h
                            (offset.x - nx) * (offset.x - nx) + (offset.y - ny) * (offset.y - ny)
                        }
                        if (closest != null) {
                            onNodeSelected(closest.name)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val brainPath = createBrainSilhouettePath(cx, cy, w, h)

            drawPath(
                path = brainPath,
                color = Color(0xFF1C2541)
            )

            clipPath(brainPath) {
                Standard1020Nodes.NODES.forEach { node ->
                    val nx = node.normX * w
                    val ny = node.normY * h

                    val isPrimaryEeg = when (deviceType) {
                        EegDeviceType.MINDWAVE_MOBILE -> node.name == "FP1"
                        EegDeviceType.BRAINLINK_LITE -> node.name == "FP1" || node.name == "FPz" || node.name == "FP2"
                        EegDeviceType.GENERIC -> node.name == "FPz"
                        EegDeviceType.PLUX -> node.name == selectedNode
                    }

                    val sensorWeight = when {
                        deviceType == EegDeviceType.BRAINLINK_LITE && (node.name == "FPz" || node.name == "FP2") -> 0.75f
                        isPrimaryEeg -> 1.0f
                        else -> 0f
                    }

                    val intensity = if (isPrimaryEeg) {
                        ((score / 100f).coerceIn(0.15f, 1f) * sensorWeight).coerceAtLeast(0.15f)
                    } else {
                        0.15f
                    }
                    val radius = (w * 0.28f * intensity) * (if (isPrimaryEeg) pulseScale else 1f)

                    val heatColors = when (target) {
                        NeurofeedbackTarget.CONCENTRATION -> listOf(
                            Color(0xFFFFD54F).copy(alpha = 0.75f * intensity),
                            Color(0xFFFF8F00).copy(alpha = 0.50f * intensity),
                            Color(0xFF0288D1).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                        NeurofeedbackTarget.MEDITATION -> listOf(
                            Color(0xFF34D399).copy(alpha = 0.75f * intensity),
                            Color(0xFF00E5FF).copy(alpha = 0.50f * intensity),
                            Color(0xFF311B92).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = heatColors,
                            center = Offset(nx, ny),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(nx, ny)
                    )
                }
            }

            drawPath(
                path = brainPath,
                color = Color(0xFF3A506B),
                style = Stroke(width = 3f)
            )

            drawLine(
                color = Color(0xFF3A506B),
                start = Offset(cx, cy - h * 0.38f),
                end = Offset(cx, cy + h * 0.38f),
                strokeWidth = 2f
            )

            // Draw frontal headband arc connecting FP1 - FPz - FP2 for BrainLink Lite
            if (deviceType == EegDeviceType.BRAINLINK_LITE) {
                val fp1 = Standard1020Nodes.NODES.first { it.name == "FP1" }
                val fpz = Standard1020Nodes.NODES.first { it.name == "FPz" }
                val fp2 = Standard1020Nodes.NODES.first { it.name == "FP2" }
                val bandPath = Path().apply {
                    moveTo(fp1.normX * w, fp1.normY * h)
                    quadraticTo(
                        fpz.normX * w, (fpz.normY - 0.03f) * h,
                        fp2.normX * w, fp2.normY * h
                    )
                }
                drawPath(
                    path = bandPath,
                    color = Color(0xFFFFB300).copy(alpha = 0.65f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            Standard1020Nodes.NODES.forEach { node ->
                val nx = node.normX * w
                val ny = node.normY * h

                val isActiveElectrode = when (deviceType) {
                    EegDeviceType.MINDWAVE_MOBILE -> node.name == "FP1"
                    EegDeviceType.BRAINLINK_LITE -> node.name == "FP1" || node.name == "FPz" || node.name == "FP2"
                    EegDeviceType.GENERIC -> node.name == "FPz"
                    EegDeviceType.PLUX -> node.name == selectedNode
                }

                val isReferenceOrGround = when (deviceType) {
                    EegDeviceType.MINDWAVE_MOBILE, EegDeviceType.GENERIC -> node.name == "A1"
                    EegDeviceType.BRAINLINK_LITE -> node.name == "FPz" || node.name == "FP2"
                    EegDeviceType.PLUX -> false
                }

                val isHighlighted = isActiveElectrode || isReferenceOrGround

                drawCircle(
                    color = if (isHighlighted) Color.White else Color(0xFF000000).copy(alpha = 0.6f),
                    radius = if (isHighlighted) 8f else 5f,
                    center = Offset(nx, ny)
                )

                if (isHighlighted) {
                    val innerColor = when {
                        deviceType == EegDeviceType.BRAINLINK_LITE -> Color(0xFFFFB300) // Gold contact (FP1, FPz, FP2)
                        isReferenceOrGround -> Color(0xFF38BDF8) // Ear clip reference (A1)
                        else -> Color(0xFF10B981) // Active EEG (FP1 / Selected)
                    }
                    drawCircle(
                        color = innerColor,
                        radius = 4.5f,
                        center = Offset(nx, ny)
                    )
                }

                val textLayout = textMeasurer.measure(
                    text = node.name,
                    style = TextStyle(
                        fontSize = (if (isHighlighted) 9 else 7).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Color.White else Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        nx - (textLayout.size.width / 2f),
                        ny + (if (isHighlighted) 10f else 6f)
                    )
                )
            }
        }
    }
}

private fun createBrainSilhouettePath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val rw = w * 0.32f
    val rh = h * 0.40f

    return Path().apply {
        moveTo(cx, cy - rh)
        cubicTo(
            cx - rw * 0.6f, cy - rh * 0.98f,
            cx - rw * 1.05f, cy - rh * 0.5f,
            cx - rw * 1.05f, cy
        )
        cubicTo(
            cx - rw * 1.05f, cy + rh * 0.5f,
            cx - rw * 0.6f, cy + rh * 0.98f,
            cx, cy + rh
        )
        cubicTo(
            cx + rw * 0.6f, cy + rh * 0.98f,
            cx + rw * 1.05f, cy + rh * 0.5f,
            cx + rw * 1.05f, cy
        )
        cubicTo(
            cx + rw * 1.05f, cy - rh * 0.5f,
            cx + rw * 0.6f, cy - rh * 0.98f,
            cx, cy - rh
        )
        close()
    }
}

/**
 * Panneau complet Neurofeedback : Vumètre Analogique avec Rémanence/Flou et Contrôles Audio
 */
@Composable
fun NeurofeedbackVuMeterPanel(
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    val target = uiState.neurofeedbackTarget
    val isConcentration = target == NeurofeedbackTarget.CONCENTRATION
    val telemetry = uiState.latestTelemetry
    val score = when (target) {
        NeurofeedbackTarget.MEDITATION -> telemetry?.meditation ?: 0
        NeurofeedbackTarget.CONCENTRATION -> telemetry?.attention ?: 0
    }

    // Motion Trail needle history (last 8 scores over time)
    val needleHistory = remember { mutableStateListOf<Int>() }

    LaunchedEffect(score) {
        needleHistory.add(score)
        if (needleHistory.size > 8) {
            needleHistory.removeAt(0)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isMeditatingState && uiState.audioFeedbackEnabled)
                Color(0xFF1B3A2A) else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: 100% Horizontal Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (uiState.audioFeedbackEnabled) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vumètre Analogique & Biofeedback Sonore",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = uiState.audioFeedbackEnabled,
                    onCheckedChange = { viewModel.toggleAudioFeedback() }
                )
            }

            // Audio Mode Chips: Continuous vs Chime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioBiofeedbackMode.entries.forEach { mode ->
                    val isSelected = uiState.audioBiofeedbackMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setAudioBiofeedbackMode(mode) },
                        label = {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Analog VU Meter Canvas
            AnalogVuMeterCanvas(
                score = score,
                target = target,
                needleHistory = needleHistory
            )

            // Audio Test & Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(
                        Locale.US,
                        "Moyenne 30s : %d%% / seuil %d%%",
                        uiState.meditationWindowPercent.toInt(),
                        uiState.meditationPercentThreshold
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        if (uiState.audioBiofeedbackMode == AudioBiofeedbackMode.CONTINUOUS) {
                            if (MeditationTonePlayer.isContinuousActive()) {
                                MeditationTonePlayer.stopContinuousTone()
                            } else {
                                MeditationTonePlayer.startContinuousTone(isConcentration)
                            }
                        } else {
                            MeditationTonePlayer.playTone(isConcentration = isConcentration)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tester le son", fontSize = 11.sp)
                }
            }

            LinearProgressIndicator(
                progress = { uiState.meditationWindowPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (isConcentration) Color(0xFFFFB300) else Color(0xFF10B981)
            )

            if (uiState.audioFeedbackEnabled) {
                Text(
                    text = "Seuil ${target.shortName} requis : ≥ ${uiState.meditationSampleThreshold}/100",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.meditationSampleThreshold.toFloat(),
                    onValueChange = { viewModel.setMeditationSampleThreshold(it.toInt()) },
                    valueRange = 20f..90f
                )

                Text(
                    text = "Pourcentage requis sur 30 secondes : ≥ ${uiState.meditationPercentThreshold}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.meditationPercentThreshold.toFloat(),
                    onValueChange = { viewModel.setMeditationPercentThreshold(it.toInt()) },
                    valueRange = 30f..100f
                )
            }
        }
    }
}

/**
 * Canvas de Vumètre Analogique avec Aiguille & Flou/Rémanence de mouvement
 */
@Composable
fun AnalogVuMeterCanvas(
    score: Int,
    target: NeurofeedbackTarget,
    needleHistory: List<Int>
) {
    val textMeasurer = rememberTextMeasurer()

    // Breathing Pacer Halo Animation (Inhale / Exhale ring)
    val infiniteTransition = rememberInfiniteTransition(label = "breathHalo")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000), // 4 seconds breathing pace
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val activeColor = when (target) {
        NeurofeedbackTarget.CONCENTRATION -> Color(0xFFFFB300) // Gold
        NeurofeedbackTarget.MEDITATION -> Color(0xFF10B981)   // Emerald Green
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val cx = w / 2f
            val cy = h * 0.88f
            val radius = w * 0.36f

            // 1. Draw Dial Arc Color Zones (Red -> Amber -> Emerald)
            val strokeWidth = 14f
            val arcRect = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(cx - radius, cy - radius)

            // Red Low Zone (0 to 40%) -> 180° to 252° (-180 to -108)
            drawArc(
                color = Color(0xFFEF4444),
                startAngle = 180f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Amber Mid Zone (40 to 70%) -> 252° to 306° (-108 to -54)
            drawArc(
                color = Color(0xFFF59E0B),
                startAngle = 252f,
                sweepAngle = 54f,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = strokeWidth)
            )

            // Emerald High Zone (70 to 100%) -> 306° to 360° (-54 to 0)
            drawArc(
                color = Color(0xFF10B981),
                startAngle = 306f,
                sweepAngle = 54f,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Draw Ticks & Numeric Readouts
            val tickValues = listOf(0, 20, 40, 60, 80, 100)
            tickValues.forEach { tVal ->
                val angleDeg = 180f + (tVal / 100f) * 180f
                val rad = Math.toRadians(angleDeg.toDouble())

                val innerR = radius - 20f
                val outerR = radius - 6f

                val x1 = cx + (cos(rad) * innerR).toFloat()
                val y1 = cy + (sin(rad) * innerR).toFloat()
                val x2 = cx + (cos(rad) * outerR).toFloat()
                val y2 = cy + (sin(rad) * outerR).toFloat()

                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 3f
                )

                // Label Text
                val tx = cx + (cos(rad) * (radius - 34f)).toFloat()
                val ty = cy + (sin(rad) * (radius - 34f)).toFloat()

                val textLayout = textMeasurer.measure(
                    text = "$tVal",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace
                    )
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        tx - (textLayout.size.width / 2f),
                        ty - (textLayout.size.height / 2f)
                    )
                )
            }

            // 3. Motion Blur / Ghost Needles Trail (Rémanence des dernières secondes)
            val historySize = needleHistory.size
            needleHistory.forEachIndexed { idx, prevScore ->
                val alpha = ((idx + 1).toFloat() / historySize.toFloat()) * 0.40f
                val ghostAngleDeg = 180f + (prevScore.coerceIn(0, 100) / 100f) * 180f
                val ghostRad = Math.toRadians(ghostAngleDeg.toDouble())

                val needleLen = radius - 25f
                val nx = cx + (cos(ghostRad) * needleLen).toFloat()
                val ny = cy + (sin(ghostRad) * needleLen).toFloat()

                drawLine(
                    color = activeColor.copy(alpha = alpha),
                    start = Offset(cx, cy),
                    end = Offset(nx, ny),
                    strokeWidth = 3f
                )
            }

            // 4. Primary Active Needle
            val currentAngleDeg = 180f + (score.coerceIn(0, 100) / 100f) * 180f
            val currentRad = Math.toRadians(currentAngleDeg.toDouble())
            val needleLen = radius - 22f

            val nx = cx + (cos(currentRad) * needleLen).toFloat()
            val ny = cy + (sin(currentRad) * needleLen).toFloat()

            // Main Needle Line
            drawLine(
                color = activeColor,
                start = Offset(cx, cy),
                end = Offset(nx, ny),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // 5. Breathing Pacer Ring (Cercle de respiration autour du pivot)
            drawCircle(
                color = activeColor.copy(alpha = 0.25f),
                radius = 24f * breathScale,
                center = Offset(cx, cy)
            )

            // Pivot Center Cap
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = activeColor,
                radius = 6f,
                center = Offset(cx, cy)
            )

            // 6. Digital Center Readout
            val digitalText = "$score%"
            val digLayout = textMeasurer.measure(
                text = digitalText,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                textLayoutResult = digLayout,
                topLeft = Offset(
                    cx - (digLayout.size.width / 2f),
                    cy - radius * 0.45f
                )
            )
        }
    }
}
