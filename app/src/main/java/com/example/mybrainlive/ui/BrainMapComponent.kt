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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybrainlive.audio.MeditationTonePlayer
import com.example.mybrainlive.bluetooth.EegDeviceType
import com.example.mybrainlive.bluetooth.EegTelemetry
import com.example.mybrainlive.bluetooth.EegUiState
import com.example.mybrainlive.bluetooth.EegViewModel
import java.util.Locale

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
        BrainNodePosition("FP1", "Frontal Pole Gauche", 0.38f, 0.20f),
        BrainNodePosition("FP2", "Frontal Pole Droit", 0.62f, 0.20f),
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
    val target = uiState.neurofeedbackTarget
    val score = when (target) {
        NeurofeedbackTarget.MEDITATION -> telemetry?.meditation ?: 0
        NeurofeedbackTarget.CONCENTRATION -> telemetry?.attention ?: 0
    }

    // Baseline reference score for comparison mode
    val baselineScore = if (uiState.eSenseHistory.isNotEmpty()) {
        when (target) {
            NeurofeedbackTarget.MEDITATION -> uiState.eSenseHistory.first().meditation
            NeurofeedbackTarget.CONCENTRATION -> uiState.eSenseHistory.first().attention
        }
    } else 35

    val activeColor = when (target) {
        NeurofeedbackTarget.CONCENTRATION -> Color(0xFFFFB300) // Gold
        NeurofeedbackTarget.MEDITATION -> Color(0xFF10B981)   // Emerald Green
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

                // Toggle Comparison View
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
                // Side-by-side comparison: Before (Baseline) vs After (Live)
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
                            deviceType = telemetry?.deviceType ?: EegDeviceType.MINDWAVE_MOBILE,
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
                            deviceType = telemetry?.deviceType ?: EegDeviceType.MINDWAVE_MOBILE,
                            target = target,
                            score = score,
                            selectedNode = selectedNodeForPlux,
                            heightDp = 180,
                            onNodeSelected = { selectedNodeForPlux = it }
                        )
                    }
                }
            } else {
                // Single Main Brain Map
                TopographicalBrainCanvas(
                    deviceType = telemetry?.deviceType ?: EegDeviceType.MINDWAVE_MOBILE,
                    target = target,
                    score = score,
                    selectedNode = selectedNodeForPlux,
                    heightDp = 240,
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
                        text = when (telemetry?.deviceType) {
                            EegDeviceType.MINDWAVE_MOBILE, EegDeviceType.BRAINLINK_LITE ->
                                "📍 Emplacement Fixe : Électrode frontale en FP1 (Front Gauche) + Clip de référence oreille A1."
                            EegDeviceType.PLUX, EegDeviceType.GENERIC ->
                                "📍 Emplacement Libre (BITalino / PLUX) : Touchez un nœud sur la carte pour repositionner le canal ($selectedNodeForPlux)."
                            null ->
                                "📍 Carte Topographique 10-20 : Touchez un nœud pour explorer les régions corticales."
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

            // Anatomical 2-Lobe Brain Silhouette Path (Left & Right Hemispheres)
            val brainPath = createBrainSilhouettePath(cx, cy, w, h)

            // 1. Fill Glassmorphic Background Silhouette
            drawPath(
                path = brainPath,
                color = Color(0xFF1C2541)
            )

            // 2. Draw Topographical Smooth Radial Heatmap Gradient per Active Node
            clipPath(brainPath) {
                Standard1020Nodes.NODES.forEach { node ->
                    val nx = node.normX * w
                    val ny = node.normY * h

                    val isActiveElectrode = when (deviceType) {
                        EegDeviceType.MINDWAVE_MOBILE, EegDeviceType.BRAINLINK_LITE -> node.name == "FP1"
                        EegDeviceType.PLUX, EegDeviceType.GENERIC -> node.name == selectedNode
                    }

                    // Intensity color gradient blending
                    val intensity = if (isActiveElectrode) (score / 100f).coerceIn(0.1f, 1f) else 0.15f
                    val radius = (w * 0.30f * intensity) * (if (isActiveElectrode) pulseScale else 1f)

                    val heatColors = when (target) {
                        NeurofeedbackTarget.CONCENTRATION -> listOf(
                            Color(0xFFFFD54F).copy(alpha = 0.75f * intensity), // Bright Gold
                            Color(0xFFFF8F00).copy(alpha = 0.50f * intensity), // Vivid Amber
                            Color(0xFF0288D1).copy(alpha = 0.20f),            // Deep Blue
                            Color.Transparent
                        )
                        NeurofeedbackTarget.MEDITATION -> listOf(
                            Color(0xFF34D399).copy(alpha = 0.75f * intensity), // Emerald Green
                            Color(0xFF00E5FF).copy(alpha = 0.50f * intensity), // Cyan
                            Color(0xFF311B92).copy(alpha = 0.20f),            // Indigo
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

            // 3. Draw Brain Hemisphere Outlines & Medial Fissure
            drawPath(
                path = brainPath,
                color = Color(0xFF3A506B),
                style = Stroke(width = 3f)
            )

            // Medial Longitudinal Fissure Line down the center
            drawLine(
                color = Color(0xFF3A506B),
                start = Offset(cx, cy - h * 0.38f),
                end = Offset(cx, cy + h * 0.38f),
                strokeWidth = 2f
            )

            // 4. Draw Crisp Electrode Nodes & Labels
            Standard1020Nodes.NODES.forEach { node ->
                val nx = node.normX * w
                val ny = node.normY * h

                val isActiveElectrode = when (deviceType) {
                    EegDeviceType.MINDWAVE_MOBILE, EegDeviceType.BRAINLINK_LITE -> node.name == "FP1"
                    EegDeviceType.PLUX, EegDeviceType.GENERIC -> node.name == selectedNode
                }

                // Electrode Node Dot
                drawCircle(
                    color = if (isActiveElectrode) Color.White else Color(0xFF000000).copy(alpha = 0.6f),
                    radius = if (isActiveElectrode) 8f else 5f,
                    center = Offset(nx, ny)
                )

                if (isActiveElectrode) {
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 4f,
                        center = Offset(nx, ny)
                    )
                }

                // Node Label Text
                val textLayout = textMeasurer.measure(
                    text = node.name,
                    style = TextStyle(
                        fontSize = (if (isActiveElectrode) 9 else 7).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActiveElectrode) Color.White else Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        nx - (textLayout.size.width / 2f),
                        ny + (if (isActiveElectrode) 10f else 6f)
                    )
                )
            }
        }
    }
}

/**
 * Creates an anatomical top-down two-hemisphere brain path outline.
 */
private fun createBrainSilhouettePath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val rw = w * 0.32f
    val rh = h * 0.40f

    return Path().apply {
        // Left Hemisphere
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

        // Right Hemisphere
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

@Composable
fun NeurofeedbackAudioControlCard(
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    val target = uiState.neurofeedbackTarget
    val isConcentration = target == NeurofeedbackTarget.CONCENTRATION

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
                        text = "Biofeedback Sonore (${target.shortName})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = uiState.audioFeedbackEnabled,
                    onCheckedChange = { viewModel.toggleAudioFeedback() }
                )
            }

            // Audio Test Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(
                        Locale.US,
                        "Fenêtre 30s : %d%% / seuil %d%%",
                        uiState.meditationWindowPercent.toInt(),
                        uiState.meditationPercentThreshold
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        MeditationTonePlayer.playTone(isConcentration = isConcentration)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tester le son", fontSize = 10.sp)
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
