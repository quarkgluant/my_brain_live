package com.example.mybrainlive.llm

import com.example.mybrainlive.bluetooth.EegTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LmStudioClient(
    private val hostIp: String = "10.0.2.2",
    private val port: Int = 1234
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeEeg(
        telemetry: EegTelemetry?,
        userPrompt: String = "Analyse mon état mental actuel et donne-moi un conseil adapté."
    ): String = withContext(Dispatchers.IO) {
        val cleanIp = hostIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val url = "http://$cleanIp:$port/v1/chat/completions"

        val systemPrompt = if (telemetry != null) {
            """
                Tu es un assistant expert en neurosciences, biofeedback et relaxation intégré dans l'application MyBrainLive.
                
                L'utilisateur porte actuellement un casque EEG de type: ${telemetry.deviceType}.
                Métriques de la session EEG en cours:
                - Niveau d'Attention (eSense 0-100): ${telemetry.attention}
                - Niveau de Méditation (eSense 0-100): ${telemetry.meditation}
                - Qualité d'ajustement du casque: ${telemetry.signalFitPercent}%
                - Bandes de fréquences EEG (spectre d'ondes):
                  * Delta (0.5-2.75 Hz, Sommeil/Régénération): ${telemetry.eegBands.delta}
                  * Theta (3.5-6.75 Hz, Relaxation profonde/Créativité): ${telemetry.eegBands.theta}
                  * Low Alpha (7.5-9.25 Hz, Repos mental): ${telemetry.eegBands.lowAlpha}
                  * High Alpha (10-11.75 Hz, Sérénité consciente): ${telemetry.eegBands.highAlpha}
                  * Low Beta (13-16.75 Hz, Éveil/Focus léger): ${telemetry.eegBands.lowBeta}
                  * High Beta (18-29.75 Hz, Concentration intense/Anxiété): ${telemetry.eegBands.highBeta}
                  * Low Gamma (31-39.75 Hz): ${telemetry.eegBands.lowGamma}
                  * Mid Gamma (41-49.75 Hz): ${telemetry.eegBands.midGamma}

                Donne une analyse synthétique, encourageante et concise (3 à 5 phrases max) en français. Explique ce que révèlent ces ondes et donne un conseil pratique.
            """.trimIndent()
        } else {
            """
                Tu es un assistant neuroscientifique et coach de biofeedback dans l'application MyBrainLive.
                Aucun casque EEG n'est actuellement connecté.
                Explique gentiment à l'utilisateur comment interpréter les métriques d'attention, de méditation et les ondes EEG (Alpha, Beta, Theta, Delta) une fois son casque connecté.
            """.trimIndent()
        }

        val jsonBody = JSONObject().apply {
            put("model", "local-model")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 400)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrBlank()) {
                        return@withContext content.trim()
                    }
                }
                "Réponse vide reçue du modèle LLM."
            } else {
                "Erreur serveur HTTP ${response.code}: ${responseBody ?: "Aucun détail"}"
            }
        } catch (e: Exception) {
            "Échec de connexion à LM Studio sur $url.\n\nPoints à vérifier:\n1. LM Studio est démarré sur votre PC et le serveur local est ON.\n2. Le Host est réglé sur 0.0.0.0 dans LM Studio.\n3. Pour un smartphone réel, utilisez l'adresse IP Wi-Fi de votre PC (ex: 192.168.1.X).\n4. Pour l'émulateur, utilisez 10.0.2.2.\n\nDétail erreur: ${e.localizedMessage}"
        }
    }
}
