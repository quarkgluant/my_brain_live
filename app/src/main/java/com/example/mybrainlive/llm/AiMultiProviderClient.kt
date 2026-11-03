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

enum class AiProvider(
    val displayName: String,
    val description: String,
    val loginUrl: String,
    val defaultModel: String
) {
    LOCAL("Local (LM Studio / Ollama)", "Exécution 100% hors-ligne sur votre PC local", "http://10.0.2.2:1234", "local-model"),
    ANTHROPIC("Anthropic (Claude)", "Modèles d'analyse avancés Claude 3.5 Sonnet", "https://console.anthropic.com/", "claude-3-5-sonnet-20241022"),
    OPENAI("OpenAI (ChatGPT)", "Modèles GPT-4o & GPT-4o-mini pour la santé", "https://platform.openai.com/api-keys", "gpt-4o-mini"),
    MISTRAL("Mistral AI", "IA européenne haute performance (Mistral Large/Small)", "https://console.mistral.ai/", "mistral-small-latest")
}

enum class AiDataSource(val label: String) {
    LIVE("📡 Télémétrie EEG Temps Réel"),
    RECORDED("📂 Session Enregistrée / Importée")
}

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AiMultiProviderClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        provider: AiProvider,
        apiKey: String,
        hostIp: String,
        port: Int,
        dataSource: AiDataSource,
        telemetry: EegTelemetry?,
        recordedSamples: List<EegTelemetry>,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {

        // Build contextual system prompt based on active data source (live or recorded session)
        val systemPrompt = buildSystemPrompt(dataSource, telemetry, recordedSamples)

        return@withContext when (provider) {
            AiProvider.LOCAL -> callOpenAiCompatibleApi(
                url = "http://${hostIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')}:$port/v1/chat/completions",
                authHeader = null,
                model = provider.defaultModel,
                systemPrompt = systemPrompt,
                userMessage = userMessage,
                history = conversationHistory
            )
            AiProvider.OPENAI -> callOpenAiCompatibleApi(
                url = "https://api.openai.com/v1/chat/completions",
                authHeader = "Bearer ${apiKey.trim()}",
                model = provider.defaultModel,
                systemPrompt = systemPrompt,
                userMessage = userMessage,
                history = conversationHistory
            )
            AiProvider.MISTRAL -> callOpenAiCompatibleApi(
                url = "https://api.mistral.ai/v1/chat/completions",
                authHeader = "Bearer ${apiKey.trim()}",
                model = provider.defaultModel,
                systemPrompt = systemPrompt,
                userMessage = userMessage,
                history = conversationHistory
            )
            AiProvider.ANTHROPIC -> callAnthropicApi(
                apiKey = apiKey.trim(),
                model = provider.defaultModel,
                systemPrompt = systemPrompt,
                userMessage = userMessage,
                history = conversationHistory
            )
        }
    }

    private fun buildSystemPrompt(
        dataSource: AiDataSource,
        telemetry: EegTelemetry?,
        recordedSamples: List<EegTelemetry>
    ): String {
        val baseHeader = "Tu es un assistant neuroscientifique et coach de biofeedback expert intégré dans l'application MyBrainLive."

        return if (dataSource == AiDataSource.LIVE) {
            if (telemetry != null) {
                """
                $baseHeader
                MODE EN DIRECT (Télémétrie EEG temps réel):
                - Appareil: ${telemetry.deviceType}
                - Attention eSense: ${telemetry.attention} / 100
                - Méditation eSense: ${telemetry.meditation} / 100
                - Ajustement Casque: ${telemetry.signalFitPercent}%
                - Bandes d'ondes EEG: Delta=${telemetry.eegBands.delta}, Theta=${telemetry.eegBands.theta}, Low Alpha=${telemetry.eegBands.lowAlpha}, High Alpha=${telemetry.eegBands.highAlpha}, Low Beta=${telemetry.eegBands.lowBeta}, High Beta=${telemetry.eegBands.highBeta}, Low Gamma=${telemetry.eegBands.lowGamma}, Mid Gamma=${telemetry.eegBands.midGamma}.
                
                Donne des conseils personnalisés, clairs et scientifiques basés sur ces métriques.
                """.trimIndent()
            } else {
                "$baseHeader Aucun casque EEG n'est connecté actuellement en direct. Réponds aux questions générales sur les ondes cérébrales et le biofeedback."
            }
        } else {
            if (recordedSamples.isNotEmpty()) {
                val avgAtt = recordedSamples.map { it.attention }.average().toInt()
                val avgMed = recordedSamples.map { it.meditation }.average().toInt()
                """
                $baseHeader
                MODE SESSION ENREGISTRÉE / IMPORTÉE:
                - Nombre d'échantillons: ${recordedSamples.size}
                - Attention moyenne: $avgAtt / 100
                - Méditation moyenne: $avgMed / 100
                - Premier échantillon: ${recordedSamples.first().deviceType}
                
                Analyse l'évolution de cette session enregistrée et réponds aux questions de l'utilisateur sur son profil neurophysiologique.
                """.trimIndent()
            } else {
                "$baseHeader Aucune session enregistrée ou importée n'est chargée actuellement."
            }
        }
    }

    private fun callOpenAiCompatibleApi(
        url: String,
        authHeader: String?,
        model: String,
        systemPrompt: String,
        userMessage: String,
        history: List<ChatMessage>
    ): String {
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            history.takeLast(6).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 500)
            put("stream", false)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

        if (!authHeader.isNullOrBlank()) {
            requestBuilder.header("Authorization", authHeader)
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val msgObj = choices.getJSONObject(0).optJSONObject("message")
                    val text = msgObj?.optString("content")
                    if (!text.isNullOrBlank()) return text.trim()
                }
                "Réponse vide reçue de l'API $url."
            } else {
                "Erreur API (${response.code}): ${body ?: "Aucun détail"}"
            }
        } catch (e: Exception) {
            "Échec de communication avec $url: ${e.localizedMessage}"
        }
    }

    private fun callAnthropicApi(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String,
        history: List<ChatMessage>
    ): String {
        if (apiKey.isBlank()) return "Veuillez saisir votre clé API Anthropic (Claude)."

        val messagesArray = JSONArray().apply {
            history.takeLast(6).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("max_tokens", 500)
            put("system", systemPrompt)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val contentArr = json.optJSONArray("content")
                if (contentArr != null && contentArr.length() > 0) {
                    val text = contentArr.getJSONObject(0).optString("text")
                    if (!text.isNullOrBlank()) return text.trim()
                }
                "Réponse vide de Claude."
            } else {
                "Erreur Anthropic API (${response.code}): ${body ?: "Aucun détail"}"
            }
        } catch (e: Exception) {
            "Échec de connexion à Anthropic API: ${e.localizedMessage}"
        }
    }
}
