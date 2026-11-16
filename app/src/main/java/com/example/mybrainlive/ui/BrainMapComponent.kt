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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.mybrainlive.bluetooth.EegDeviceType
import com.example.mybrainlive.bluetooth.EegTelemetry
import com.example.mybrainlive.bluetooth.EegUiState
import com.example.mybrainlive.bluetooth.EegViewModel
import java.util.Locale

enum class NeurofeedbackTarget(val label: String, val shortName: String) {
    MEDITATION("🧘 Méditation (Calme & Sérénité)", "Méditation"),
    CONCENTRATION("🎯 Concentration (Focus & Attention)", "Concentration")
}

// 10-20 Standard Positions on Normalized 2D Grid (x: 0..1, y: 0..1)
data class BrainNodePosition(
    val name: String,
    val description: String,
    val normX: Float,
    val normY: Float
)

object Standard1020Nodes {
    val NODES = listOf(
        BrainNodePosition("FP1", "Frontal Pole Gauche (Att/Méd)", 0.38f, 0.20f),
        BrainNodePosition("FP2", "Frontal Pole Droit", 0.62f, 0.20f),
        BrainNodePosition("F3", "Frontal Gauche", 0.32f, 0.35f),
        BrainNodePosition("Fz", "Frontal Médian (Focus)", 0.50f, 0.35f),
        BrainNodePosition("F4", "Frontal Droit", 0.68f, 0.35f),
        BrainNodePosition("C3", "Central Gauche (Moteur)", 0.28f, 0.50f),
        BrainNodePosition("Cz", "Sommet Cérébral", 0.50f, 0.50f),
        BrainNodePosition("C4", "Central Droit", 0.72f, 0.50f),
        BrainNodePosition("P3", "Pariétal Gauche", 0.34f, 0.65f),
        BrainNodePosition("Pz", "Pariétal Médian", 0.50f, 0.65f),
        BrainNodePosition("P4", "Pariétal Droit", 0.66f, 0.65f),
        BrainNodePosition("O1", "Occipital Gauche (Alpha)", 0.38f, 0.80f),
        BrainNodePosition("O2", "Occipital Droit (Alpha)", 0.62f, 0.80f),
        BrainNodePosition("A1", "Oreille Gauche (Référence)", 0.12f, 0.50f),
        BrainNodePosition("A2", "Oreille Droite (Masse)", 0.88f, 0.50f)
    )
}

@Composable
fun InteractiveBrainMapCard(
    uiState: EegUiState,
    viewModel: EegViewModel
) {
    var selectedNodeForPlux by remember { mutableStateOf("FP1") }

    val telemetry = uiState.latestTelemetry
    val target = uiState.neurofeedbackTarget
    val score = when (target) {
        NeurofeedbackTarget.MEDITATION -> telemetry?.meditation ?: 0
        NeurofeedbackTarget.CONCENTRATION -> telemetry?.attention ?: 0
    }

    val activeColor = when (target) {
        NeurofeedbackTarget.CONCENTRATION -> Color(0xFFFFB300) // Gold/Yellow for Concentration
        NeurofeedbackTarget.MEDITATION -> Color(0xFF10B981)   // Emerald Green for Meditation
    }

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
                        text = "Carte 10-20 & Halo de Neurofeedback",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                EducationalHelpButton(topic = EducationalTopics.RAW_WAVE)
            }

            // Target Selector: Concentration vs Méditation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }

            // Live score indicator
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
                Text(
                    text = if (telemetry?.isHeadsetConnected == true) "Signal Actif" else "Mode Démo / Inactif",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = activeColor
            )

            // Interactive Canvas Skull Map
            BrainMapCanvas(
                deviceType = telemetry?.deviceType ?: EegDeviceType.MINDWAVE_MOBILE,
                target = target,
                score = score,
                selectedNode = selectedNodeForPlux,
                onNodeSelected = { nodeName -> selectedNodeForPlux = nodeName }
            )

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
                                "📍 Emplacement Fixe : Électrode frontale positionnée en FP1 (Front Gauche) + Clip de référence oreille A1."
                            EegDeviceType.PLUX, EegDeviceType.GENERIC ->
                                "📍 Emplacement Libre (BITalino / PLUX) : Touchez un nœud 10-20 sur la carte pour repositionner le canal ($selectedNodeForPlux)."
                            null ->
                                "📍 Emplacement Standard (10-20) : Touchez la carte pour explorer les régions cérébrales."
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
fun BrainMapCanvas(
    deviceType: EegDeviceType,
    target: NeurofeedbackTarget,
    score: Int,
    selectedNode: String,
    onNodeSelected: (String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    // Pulse animation for active halo
    val infiniteTransition = rememberInfiniteTransition(label = "haloPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activeColor = when (target) {
        NeurofeedbackTarget.CONCENTRATION -> Color(0xFFFFC107) // Gold/Yellow
        NeurofeedbackTarget.MEDITATION -> Color(0xFF10B981)   // Emerald Green
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        // Find closest node to tap offset
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
            val headRadiusX = w * 0.36f
            val headRadiusY = h * 0.42f

            // 1. Draw Skull Outline (Nose + Ears + Head)
            // Nose triangle at top
            val nosePath = Path().apply {
                moveTo(cx - 16f, cy - headRadiusY + 4f)
                lineTo(cx, cy - headRadiusY - 18f)
                lineTo(cx + 16f, cy - headRadiusY + 4f)
                close()
            }
            drawPath(nosePath, color = Color(0xFF475569), style = Stroke(width = 3f))

            // Left Ear
            drawOval(
                color = Color(0xFF475569),
                topLeft = Offset(cx - headRadiusX - 14f, cy - 20f),
                size = Size(14f, 40f),
                style = Stroke(width = 3f)
            )

            // Right Ear
            drawOval(
                color = Color(0xFF475569),
                topLeft = Offset(cx + headRadiusX, cy - 20f),
                size = Size(14f, 40f),
                style = Stroke(width = 3f)
            )

            // Main Head Outline
            drawOval(
                color = Color(0xFF334155),
                topLeft = Offset(cx - headRadiusX, cy - headRadiusY),
                size = Size(headRadiusX * 2f, headRadiusY * 2f),
                style = Stroke(width = 3f)
            )

            // 2. Draw 10-20 Grid Lines
            val gridColor = Color(0xFF1E293B)
            drawLine(gridColor, Offset(cx - headRadiusX, cy), Offset(cx + headRadiusX, cy), strokeWidth = 2f)
            drawLine(gridColor, Offset(cx, cy - headRadiusY), Offset(cx, cy + headRadiusY), strokeWidth = 2f)

            // 3. Render 10-20 Nodes & Heatmap Glow
            Standard1020Nodes.NODES.forEach { node ->
                val nx = node.normX * w
                val ny = node.normY * h

                val isActiveElectrode = when (deviceType) {
                    EegDeviceType.MINDWAVE_MOBILE, EegDeviceType.BRAINLINK_LITE -> node.name == "FP1"
                    EegDeviceType.PLUX, EegDeviceType.GENERIC -> node.name == selectedNode
                }

                // Halo Effect if Active Electrode
                if (isActiveElectrode) {
                    val haloRadius = (22f + (score * 0.25f)) * pulseScale
                    drawCircle(
                        color = activeColor.copy(alpha = 0.35f),
                        radius = haloRadius,
                        center = Offset(nx, ny)
                    )
                    drawCircle(
                        color = activeColor.copy(alpha = 0.15f),
                        radius = haloRadius * 1.5f,
                        center = Offset(nx, ny)
                    )
                }

                // Node Circle
                val circleColor = if (isActiveElectrode) activeColor else Color(0xFF475569)
                val nodeRadius = if (isActiveElectrode) 14f else 10f

                drawCircle(
                    color = circleColor,
                    radius = nodeRadius,
                    center = Offset(nx, ny)
                )

                // Label Text inside/above node
                val textLayoutResult = textMeasurer.measure(
                    text = node.name,
                    style = TextStyle(
                        fontSize = (if (isActiveElectrode) 10 else 8).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActiveElectrode) Color.Black else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        nx - (textLayoutResult.size.width / 2f),
                        ny - (textLayoutResult.size.height / 2f)
                    )
                )
            }
        }
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
