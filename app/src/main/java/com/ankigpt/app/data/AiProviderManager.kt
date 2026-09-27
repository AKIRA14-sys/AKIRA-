package com.ankigpt.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

enum class AiProviderType(val displayName: String) {
    OPENROUTER("OpenRouter"),
    GROQ("Groq"),
    GEMINI("Gemini")
}

class AiProviderManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun listAvailableModels(provider: AiProviderType, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val models = mutableListOf<String>()
        if (apiKey.isBlank()) return@withContext defaultModelsForProvider(provider)

        try {
            when (provider) {
                AiProviderType.GROQ -> {
                    val request = Request.Builder()
                        .url("https://api.groq.com/openai/v1/models")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = JSONObject(response.body?.string() ?: "")
                            val data = json.optJSONArray("data")
                            if (data != null) {
                                for (i in 0 until data.length()) {
                                    val id = data.getJSONObject(i).optString("id")
                                    if (id.isNotEmpty()) models.add(id)
                                }
                            }
                        }
                    }
                }
                AiProviderType.OPENROUTER -> {
                    val request = Request.Builder()
                        .url("https://openrouter.ai/api/v1/models")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = JSONObject(response.body?.string() ?: "")
                            val data = json.optJSONArray("data")
                            if (data != null) {
                                for (i in 0 until data.length().coerceAtMost(25)) {
                                    val id = data.getJSONObject(i).optString("id")
                                    if (id.isNotEmpty()) models.add(id)
                                }
                            }
                        }
                    }
                }
                AiProviderType.GEMINI -> {
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = JSONObject(response.body?.string() ?: "")
                            val data = json.optJSONArray("models")
                            if (data != null) {
                                for (i in 0 until data.length()) {
                                    val name = data.getJSONObject(i).optString("name")
                                    if (name.contains("gemini")) {
                                        models.add(name.removePrefix("models/"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (models.isEmpty()) {
            models.addAll(defaultModelsForProvider(provider))
        }

        return@withContext models
    }

    fun defaultModelsForProvider(provider: AiProviderType): List<String> {
        return when (provider) {
            AiProviderType.GROQ -> listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
            AiProviderType.OPENROUTER -> listOf("deepseek/deepseek-r1:free", "google/gemini-2.0-flash-lite-preview-02-05:free", "meta-llama/llama-3.3-70b-instruct:free")
            AiProviderType.GEMINI -> listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash")
        }
    }

    fun streamChatCompletion(
        provider: AiProviderType,
        apiKey: String,
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>
    ): Flow<String> = flow {
        val endpoint = when (provider) {
            AiProviderType.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            AiProviderType.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/chat/completions"
        }

        val jsonBody = JSONObject().apply {
            put("model", model.ifBlank { defaultModelsForProvider(provider).first() })
            put("stream", true)

            val msgArray = JSONArray()
            if (systemPrompt.isNotBlank()) {
                msgArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            for (msg in messages) {
                msgArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", msgArray)
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Content-Type", "application/json")

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "HTTP ${response.code}"
                throw Exception("[$provider Error ${response.code}]: $errorMsg")
            }

            val body = response.body ?: throw Exception("Empty response body from $provider")
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
