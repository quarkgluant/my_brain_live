package com.example.mybrainlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class HelpLevel(val title: String, val iconColor: Color) {
    BEGINNER("🟢 Débutant", Color(0xFF2E7D32)),
    INTERMEDIATE("🟡 Intermédiaire", Color(0xFFF57C00)),
    ADVANCED("🔴 Avancé (Scientifique)", Color(0xFFC62828))
}

data class EducationalTopic(
    val title: String,
    val beginnerText: String,
    val intermediateText: String,
    val advancedText: String,
    val scientificReferences: List<String> = emptyList()
)

object EducationalTopics {

    val RAW_WAVE = EducationalTopic(
        title = "Oscilloscope & Onde Brute EEG",
        beginnerText = "L'oscilloscope montre l'activité électrique produite par votre cerveau en temps réel. Chaque pic représente de minuscules variations de tension mesurées en microvolts (µV).",
        intermediateText = "L'onde brute correspond au signal électroencéphalographique non filtré capté par l'électrode frontale (ex: FP1). L'amplitude varie généralement entre -200 µV et +200 µV. Les battements de cils et mouvements musculaires provoquent de grands pics (artefacts).",
        advancedText = "Signal potentiel postsynaptique généralisé sommé à la surface du scalp. Représente la sommation spatio-temporelle des potentiels postsynaptiques denses des neurones pyramidaux corticaux.",
        scientificReferences = listOf(
            "Pfurtscheller, G., & Lopes da Silva, F. H. (1999). Event-related EEG/MEG synchronization and desynchronization. Clinical Neurophysiology, 110(11), 1842-1857.",
            "Nunez, P. L., & Srinivasan, R. (2006). Electric fields of the brain: the neurophysics of EEG. Oxford University Press."
        )
    )

    val ATTENTION_MEDITATION = EducationalTopic(
        title = "Indices eSense : Attention & Méditation",
        beginnerText = "L'Attention (0 à 100) mesure votre degré de concentration active sur une tâche. La Méditation (0 à 100) mesure votre niveau de calme mental et de sérénité.",
        intermediateText = "Algorithme eSense calculé à partir de la puissance relative des bandes de fréquences. L'Attention augmente lors du calcul mental et de la focalisation (hausse des ondes Beta). La Méditation augmente lors du relâchement musculaire et de la respiration calme (hausse des ondes Alpha et Theta).",
        advancedText = "La desynchronisation liée à l'évènement (ERD) dans la bande Alpha (8-12 Hz) est l'indicateur neurophysiologique de l'attention sélective et du traitement de l'information. L'inhibition active des zones non pertinentes est médiée par une hausse d'amplitude Alpha (ERS parietal/occipital).",
        scientificReferences = listOf(
            "Klimesch, W. (1999). EEG alpha and theta oscillations reflect cognitive and memory performance: a review and analysis. Brain Research Reviews, 29(2-3), 169-195.",
            "Jensen, O., & Mazaheri, A. (2010). Shaping functional architecture by oscillatory alpha power: gating by inhibition. Frontiers in Human Neuroscience, 4, 186.",
            "Barry, R. J., et al. (2003). EEG differences between eyes-closed and eyes-open resting conditions. Clinical Neurophysiology, 114(10), 1807-1815."
        )
    )

    val SPECTRUM = EducationalTopic(
        title = "Spectre de Fréquences (Ondes Cérébrales)",
        beginnerText = "Votre cerveau émet différentes ondes selon votre état : Delta (sommeil), Theta (rêverie/méditation), Alpha (repos calme), Beta (réflexion/focus), et Gamma (haute performance).",
        intermediateText = "Décomposition par Transformée de Fourier (FFT) en 8 bandes de fréquences : Delta (0.5-2.75 Hz), Theta (3.5-6.75 Hz), Low Alpha (7.5-9.25 Hz), High Alpha (10-11.75 Hz), Low Beta (13-16.75 Hz), High Beta (18-29.75 Hz), Low Gamma (31-39.75 Hz), Mid Gamma (41-49.75 Hz).",
        advancedText = "Les oscillateurs thalamo-corticaux génèrent des rythmes distincts : le rythme Theta frontal médial (4-8 Hz) reflète la charge de travail de la mémoire de travail (Gevins et al., 1997). La bande Gamma (30-80 Hz) soutient la liaison d'informations perceptives (feature binding) et l'intégration neuronale à longue distance.",
        scientificReferences = listOf(
            "Gevins, A., et al. (1997). High-resolution EEG mapping of cortical activation related to working memory: effects of task difficulty, type of processing, and practice. Cerebral Cortex, 7(4), 374-385.",
            "Buzsáki, G., & Draguhn, A. (2004). Neuronal oscillations in cortical networks. Science, 304(5679), 1926-1929.",
            "Lotte, F., et al. (2018). A review of classification algorithms for EEG-based brain-computer interfaces. Journal of Neural Engineering, 15(3), 031005."
        )
    )

    val BIOFEEDBACK_AUDIO = EducationalTopic(
        title = "Retour Sonore de Méditation (Biofeedback)",
        beginnerText = "L'application joue un son apaisant (bol tibétain) dès que vous maintenez un état de méditation profond pendant au moins 30 secondes.",
        intermediateText = "Système de boucle de rétroaction neuronale en temps réel (Neurofeedback). Il calcule la moyenne glissante de vos données de méditation eSense sur une fenêtre de 30 secondes.",
        advancedText = "Le conditionnement opérant neurophysiologique permet d'entraîner la plasticité synaptique et la régulation du système nerveux autonome (SNA) par biofeedback acoustique. Favorise la cohérence cardiaque et l'augmentation des rythmes Alpha/Theta.",
        scientificReferences = listOf(
            "Kamiya, J. (1969). Operant control of the EEG alpha rhythm and some of its reported effects on consciousness. Altered States of Consciousness, 507-517.",
            "Tei, S., et al. (2009). Meditators have higher insulative cortical thickness and enhanced theta/alpha synchrony. NeuroImage, 47, S120."
        )
    )

    val LLM_ANALYSIS = EducationalTopic(
        title = "Analyse & Feedback Neuro-IA",
        beginnerText = "L'intelligence artificielle analyse la combinaison de vos ondes cérébrales et vous explique clairement votre état mental avec des conseils pratiques.",
        intermediateText = "Le modèle de langage (LLM) interprète les métriques spectrales de votre session pour synthétiser des conseils neuro-ergonomiques et de gestion du stress adaptés à votre profil.",
        advancedText = "Modélisation sémantique guidée par des prompts structurés à partir de descripteurs spectraux EEG. Permet l'interprétabilité qualitative des biomarqueurs électrophysiologiques.",
        scientificReferences = listOf(
            "Lotte, F., et al. (2018). A review of classification algorithms for EEG-based brain-computer interfaces. Journal of Neural Engineering, 15(3), 031005."
        )
    )
}

@Composable
fun EducationalHelpButton(
    topic: EducationalTopic,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Aide pédagogique : ${topic.title}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDialog) {
        EducationalHelpDialog(
            topic = topic,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun EducationalHelpDialog(
    topic: EducationalTopic,
    onDismiss: () -> Unit
) {
    var selectedLevel by remember { mutableStateOf(HelpLevel.BEGINNER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Selector Row: Débutant / Intermédiaire / Avancé
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HelpLevel.entries.forEach { level ->
                        val isSelected = selectedLevel == level
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) level.iconColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLevel = level }
                        ) {
                            Text(
                                text = when (level) {
                                    HelpLevel.BEGINNER -> "Débutant"
                                    HelpLevel.INTERMEDIATE -> "Intermédiaire"
                                    HelpLevel.ADVANCED -> "Avancé"
                                },
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Level Content Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (selectedLevel) {
                                    HelpLevel.BEGINNER -> Icons.Default.MenuBook
                                    HelpLevel.INTERMEDIATE -> Icons.Default.School
                                    HelpLevel.ADVANCED -> Icons.Default.Science
                                },
                                contentDescription = null,
                                tint = selectedLevel.iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedLevel.title,
                                fontWeight = FontWeight.Bold,
                                color = selectedLevel.iconColor,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Text(
                            text = when (selectedLevel) {
                                HelpLevel.BEGINNER -> topic.beginnerText
                                HelpLevel.INTERMEDIATE -> topic.intermediateText
                                HelpLevel.ADVANCED -> topic.advancedText
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Scientific References for Advanced Level
                        if (selectedLevel == HelpLevel.ADVANCED && topic.scientificReferences.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "📚 RÉFÉRENCES SCIENTIFIQUES :",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    topic.scientificReferences.forEach { ref ->
                                        Text(
                                            text = "• $ref",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Compris")
            }
        }
    )
}
