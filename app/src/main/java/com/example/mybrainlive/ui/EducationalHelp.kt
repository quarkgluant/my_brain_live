package com.example.mybrainlive.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class HelpLevel(val title: String, val iconColor: Color) {
    BEGINNER("🟢 Débutant", Color(0xFF2E7D32)),
    INTERMEDIATE("🟡 Intermédiaire", Color(0xFFF57C00)),
    ADVANCED("🔴 Avancé (Scientifique)", Color(0xFFC62828))
}

data class EducationalTopic(
    val title: String,
    val beginnerSteps: List<String>,
    val intermediateSteps: List<String>,
    val advancedSteps: List<String>,
    val scientificReferences: List<String> = emptyList()
)

object EducationalTopics {

    val RAW_WAVE = EducationalTopic(
        title = "Oscilloscope & Onde Brute EEG",
        beginnerSteps = listOf(
            "L'oscilloscope montre l'activité électrique produite par votre cerveau en temps réel. Chaque pic représente de minuscules variations de tension mesurées en microvolts (µV).",
            "💡 Comment lire la courbe ? Quand vous êtes calme et détendu, l'onde forme un dessin fluide et plutôt régulier. Si vous clignez des yeux, serrez les dents ou bougez la tête, vous verrez de grands pics verticaux : ce sont des signaux musculaires.",
            "🧘 Pour obtenir une mesure optimale : installez-vous confortablement, détendez vos muscles faciaux et la mâchoire, et observez comment la courbe se stabilise !"
        ),
        intermediateSteps = listOf(
            "L'onde brute correspond au signal électroencéphalographique non filtré capté sur le front (bandeau frontal à 3 électrodes FP1/FPz/FP2 pour BrainLink Lite, FP1 + clip oreille A1 pour MindWave, ou FPz fronto-médian + clip oreille A1 pour le capteur générique). L'amplitude varie généralement entre -200 µV et +200 µV à une fréquence d'échantillonnage de 512 Hz.",
            "⚡ Gestion des artefacts : Les potentiels électromyographiques (EMG, contraction de la mâchoire) et électro-oculographiques (EOG, clignements) ont des amplitudes 5 à 10 fois supérieures au signal cérébral.",
            "📊 Transformation mathématique : Le signal brut temporel (µV) est découpé en fenêtres glissantes d'une seconde pour alimenter la Transformée de Fourier Rapide (FFT) qui extrait la puissance des bandes de fréquences (Delta, Theta, Alpha, Beta, Gamma)."
        ),
        advancedSteps = listOf(
            "Signal potentiel postsynaptique généralisé sommé à la surface du scalp. Représente la sommation spatio-temporelle des potentiels postsynaptiques denses des neurones pyramidaux corticaux.",
            "🔬 Biophysique du signal : La résistance de contact électrode-peau (impédance < 5 kΩ) détermine le rapport signal/bruit (SNR). Les dipôles électriques sont générés par le flux d'ions sodiques et potassiques lors des potentiels postsynaptiques inhibiteurs (PPSI) et excitateurs (PPSE).",
            "📐 Traitement numérique : Les signaux sont fenêtrés (fenêtre de Hanning/Hamming) pour minimiser la fuite spectrale avant d'extraire la densité spectrale de puissance (PSD en µV²/Hz)."
        ),
        scientificReferences = listOf(
            "Pfurtscheller, G., & Lopes da Silva, F. H. (1999). Event-related EEG/MEG synchronization and desynchronization. Clinical Neurophysiology, 110(11), 1842-1857.",
            "Nunez, P. L., & Srinivasan, R. (2006). Electric fields of the brain: the neurophysics of EEG. Oxford University Press."
        )
    )

    val ATTENTION_MEDITATION = EducationalTopic(
        title = "Indices eSense : Attention & Méditation",
        beginnerSteps = listOf(
            "L'Attention (0 à 100) mesure votre degré de concentration active sur une tâche. La Méditation (0 à 100) mesure votre niveau de calme mental et de sérénité.",
            "🎯 Comprendre vos scores : 40 à 60 est la zone neutre normale. Au-dessus de 60, vous êtes dans un état actif (focus fort ou calme profond). Au-dessus de 80, vous êtes en état de performance optimale (Flow).",
            "🚀 Entraînement : Pour faire monter l'Attention, fixez un point visuel ou résolvez un problème mental. Pour faire monter la Méditation, fermez les yeux, ralentissez la respiration et relâchez vos épaules."
        ),
        intermediateSteps = listOf(
            "Algorithme eSense calculé à partir de la puissance relative des bandes de fréquences. L'Attention augmente lors du calcul mental et de la focalisation (hausse des ondes Beta). La Méditation augmente lors du relâchement musculaire et de la respiration calme (hausse des ondes Alpha et Theta).",
            "🧠 Neurobiologie de l'Attention : La bascule entre le réseau attentionnel ventral/dorsal et le réseau de mode par défaut (DMN) s'accompagne d'une modulation réciproque des oscillateurs Alpha et Beta.",
            "🎧 Boucle fermée : Le biofeedback audio et visuel permet de renforcer par conditionnement opérant les états d'éveil calme (High Alpha / Low Beta) réduisant la fatigue cognitive."
        ),
        advancedSteps = listOf(
            "La desynchronisation liée à l'évènement (ERD) dans la bande Alpha (8-12 Hz) est l'indicateur neurophysiologique de l'attention sélective et du traitement de l'information. L'inhibition active des zones non pertinentes est médiée par une hausse d'amplitude Alpha (ERS parietal/occipital).",
            "🔬 Hypothèse de Gating par Inhibition (Jensen & Mazaheri, 2010) : L'amplitude Alpha contrôle le flux d'informations en fermant les régions cérébrales non sollicitées via des boucles d'inhibition GABAergiques interneuronales.",
            "📊 Ratios cliniques : Le ratio Theta/Beta (TBR) sur Fz/Cz est un biomarqueur validé par la FDA pour l'évaluation de la charge attentionnelle et du contrôle exécutif."
        ),
        scientificReferences = listOf(
            "Klimesch, W. (1999). EEG alpha and theta oscillations reflect cognitive and memory performance. Brain Research Reviews, 29(2-3), 169-195.",
            "Jensen, O., & Mazaheri, A. (2010). Shaping functional architecture by oscillatory alpha power: gating by inhibition. Frontiers in Human Neuroscience, 4, 186.",
            "Barry, R. J., et al. (2003). EEG differences between eyes-closed and eyes-open resting conditions. Clinical Neurophysiology, 114(10), 1807-1815."
        )
    )

    val SPECTRUM = EducationalTopic(
        title = "Spectre de Fréquences (Ondes Cérébrales)",
        beginnerSteps = listOf(
            "Votre cerveau émet différentes ondes selon votre état : Delta (sommeil), Theta (rêverie/méditation), Alpha (repos calme), Beta (réflexion/focus), et Gamma (haute performance).",
            "🌊 Le profil de vos ondes : Delta = régénération ; Theta = intuition & créativité ; Alpha = le 'moteur au ralenti' calme mais éveillé ; Beta = l'action et le calcul ; Gamma = la super-concentration.",
            "☯️ Trouver l'équilibre : L'objectif n'est pas d'éteindre une onde, mais d'adapter la domination fréquentielle à l'activité souhaitée (ex: Alpha/Theta pour méditer, Low Beta pour étudier)."
        ),
        intermediateSteps = listOf(
            "Décomposition par Transformée de Fourier (FFT) en 8 bandes de fréquences : Delta (0.5-2.75 Hz), Theta (3.5-6.75 Hz), Low Alpha (7.5-9.25 Hz), High Alpha (10-11.75 Hz), Low Beta (13-16.75 Hz), High Beta (18-29.75 Hz), Low Gamma (31-39.75 Hz), Mid Gamma (41-49.75 Hz).",
            "🔬 Distinction Alpha / Beta : Low Alpha (repos passif) vs High Alpha (sérénité alerte). Low Beta (focus détendu sans stress) vs High Beta (hyper-vigilance, anxiété, rumination).",
            "🎯 Protocole de Biofeedback : Viser l'augmentation du ratio High Alpha / High Beta pour réduire le stress sans perte de vigilance cognitive."
        ),
        advancedSteps = listOf(
            "Les oscillateurs thalamo-corticaux génèrent des rythmes distincts : le rythme Theta frontal médial (4-8 Hz) reflète la charge de travail de la mémoire de travail (Gevins et al., 1997). La bande Gamma (30-80 Hz) soutient la liaison d'informations perceptives (feature binding) et l'intégration neuronale à longue distance.",
            "🔬 Synchronisation Gamma (30-80 Hz) : Soutient le 'feature binding' (intégration multisensorielle) via la synchronie de phase à longue distance entre réseaux d'interneurones GABAergiques à décharge rapide (FS basket cells).",
            "📐 Couplage Phase-Amplitude (PAC) : La phase de l'onde lente Theta module l'amplitude de l'onde rapide Gamma dans l'hippocampe et le cortex préfrontal lors de la consolidation mémorielle."
        ),
        scientificReferences = listOf(
            "Gevins, A., et al. (1997). High-resolution EEG mapping of cortical activation related to working memory. Cerebral Cortex, 7(4), 374-385.",
            "Buzsáki, G., & Draguhn, A. (2004). Neuronal oscillations in cortical networks. Science, 304(5679), 1926-1929.",
            "Lotte, F., et al. (2018). A review of classification algorithms for EEG-based brain-computer interfaces. Journal of Neural Engineering, 15(3), 031005."
        )
    )

    val BIOFEEDBACK_AUDIO = EducationalTopic(
        title = "Retour Sonore de Méditation (Biofeedback)",
        beginnerSteps = listOf(
            "L'application joue un son apaisant (bol tibétain ou carillon clair) dès que vous maintenez un état de méditation ou de concentration profond pendant au moins 30 secondes.",
            "🎵 Pourquoi un son ? Le son sert de récompense immédiate pour votre cerveau. Sans regarder l'écran, vous savez instantanément que vous avez atteint le bon état d'esprit.",
            "🧘 Entraînement au calme : Fermez les yeux, écoutez votre respiration et cherchez à déclencher le son de plus en plus souvent au cours de votre séance de 5 minutes."
        ),
        intermediateSteps = listOf(
            "Système de boucle de rétroaction neuronale en temps réel (Neurofeedback). Il calcule la moyenne glissante de vos données sur une fenêtre de 30 secondes et déclenche une réponse synthétique 432 Hz / 528 Hz.",
            "🎶 Acoustique des tonalités : Le mode Méditation génère un bol tibétain à 432 Hz avec décroissance exponentielle. Le mode Concentration génère un carillon cristallin à 528 Hz.",
            "⚙️ Personnalisation : Ajustez le seuil eSense (ex: 60/100) et le pourcentage de maintien sur 30s (ex: 70%) selon votre niveau d'expérience."
        ),
        advancedSteps = listOf(
            "Le conditionnement opérant neurophysiologique permet d'entraîner la plasticité synaptique et la régulation du système nerveux autonome (SNA) par biofeedback acoustique.",
            "🔬 Régulation Vagale : Le neurofeedback audio stimule le tonus vagal (SNA parasympathique), mesurable par l'augmentation de la variabilité de la fréquence cardiaque (HRV) simultanée.",
            "📊 Protocoles Alpha/Theta (Peniston & Kulkosky) : Entraînement à la baisse de l'éveil sympathique avec facilitation de la cohérence électroencéphalographique."
        ),
        scientificReferences = listOf(
            "Kamiya, J. (1969). Operant control of the EEG alpha rhythm and some of its reported effects on consciousness. Altered States of Consciousness, 507-517.",
            "Tei, S., et al. (2009). Meditators have higher insulative cortical thickness and enhanced theta/alpha synchrony. NeuroImage, 47, S120."
        )
    )

    val LLM_ANALYSIS = EducationalTopic(
        title = "Analyse & Feedback Neuro-IA",
        beginnerSteps = listOf(
            "L'intelligence artificielle analyse la combinaison de vos ondes cérébrales et vous explique clairement votre état mental avec des conseils pratiques.",
            "💡 Une interprétation humaine : Au lieu de simples chiffres bruts, l'IA traduit vos données EEG en mots simples : 'Votre niveau de calme est excellent, mais votre attention faiblit...'.",
            "💬 Mode discussion : Vous pouvez poser toutes vos questions à l'IA : 'Comment améliorer ma concentration ?', 'Pourquoi mes ondes Theta sont élevées ?'."
        ),
        intermediateSteps = listOf(
            "Le modèle de langage (LLM) interprète les métriques spectrales de votre session pour synthétiser des conseils neuro-ergonomiques et de gestion du stress adaptés à votre profil.",
            "🌐 Choix du fournisseur : Vous pouvez utiliser un modèle 100% local sur votre PC (LM Studio/Ollama) sans qu'aucune donnée ne quitte votre réseau, ou utiliser Claude, ChatGPT ou Mistral AI.",
            "📂 Analyse de sessions enregistrées : Vous pouvez charger un fichier CSV/JSON de session passée et demander à l'IA d'analyser l'ensemble de votre courbe de méditation."
        ),
        advancedSteps = listOf(
            "Modélisation sémantique guidée par des prompts structurés à partir de descripteurs spectraux EEG. Permet l'interprétabilité qualitative des biomarqueurs électrophysiologiques.",
            "🔬 Ingestion de données neurophysiologiques : Injection contextuelle des bandes de puissance spectrale (PSD), des indices eSense et des ratios spectraux dans l'espace latent du modèle.",
            "🔐 Confidentialité des biomarqueurs : L'inférence locale via LM Studio sur architecture Llama/Qwen garantit l'étanchéité totale des données de santé électrophysiologiques."
        ),
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

    // Multi-step expansion state per level (0 = overview, 1 = deep dive, 2 = advanced details...)
    var beginnerStep by remember { mutableIntStateOf(0) }
    var intermediateStep by remember { mutableIntStateOf(0) }
    var advancedStep by remember { mutableIntStateOf(0) }

    val currentStep = when (selectedLevel) {
        HelpLevel.BEGINNER -> beginnerStep
        HelpLevel.INTERMEDIATE -> intermediateStep
        HelpLevel.ADVANCED -> advancedStep
    }

    val currentStepsList = when (selectedLevel) {
        HelpLevel.BEGINNER -> topic.beginnerSteps
        HelpLevel.INTERMEDIATE -> topic.intermediateSteps
        HelpLevel.ADVANCED -> topic.advancedSteps
    }

    val scrollState = rememberScrollState()

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Card showing revealed steps
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (selectedLevel) {
                                    HelpLevel.BEGINNER -> Icons.AutoMirrored.Filled.MenuBook
                                    HelpLevel.INTERMEDIATE -> Icons.Default.School
                                    HelpLevel.ADVANCED -> Icons.Default.Science
                                },
                                contentDescription = null,
                                tint = selectedLevel.iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedLevel.title} (Étape ${currentStep + 1}/${currentStepsList.size})",
                                fontWeight = FontWeight.Bold,
                                color = selectedLevel.iconColor,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // Display all steps revealed so far
                        for (i in 0..currentStep.coerceAtMost(currentStepsList.lastIndex)) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically()
                            ) {
                                Surface(
                                    color = if (i == 0) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (i > 0) {
                                            Text(
                                                text = "🔎 DÉTAILS AVANCÉS - PARTIE $i :",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = selectedLevel.iconColor,
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Text(
                                            text = currentStepsList[i],
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Progressive "En savoir plus" Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep < currentStepsList.lastIndex) {
                                Button(
                                    onClick = {
                                        when (selectedLevel) {
                                            HelpLevel.BEGINNER -> beginnerStep++
                                            HelpLevel.INTERMEDIATE -> intermediateStep++
                                            HelpLevel.ADVANCED -> advancedStep++
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "En savoir plus (Étape ${currentStep + 2}/${currentStepsList.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = {
                                        when (selectedLevel) {
                                            HelpLevel.BEGINNER -> beginnerStep = 0
                                            HelpLevel.INTERMEDIATE -> intermediateStep = 0
                                            HelpLevel.ADVANCED -> advancedStep = 0
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Réduire les détails", fontSize = 11.sp)
                                }
                            }
                        }

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
