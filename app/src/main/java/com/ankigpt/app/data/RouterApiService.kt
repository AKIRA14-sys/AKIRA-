package com.ankigpt.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: String
)

class RouterApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamChatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>
    ): Flow<String> = flow {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.endsWith("/chat/completions")) cleanBaseUrl else "$cleanBaseUrl/chat/completions"

        val jsonBody = JSONObject()
        jsonBody.put("model", model.ifBlank { "deepseek/deepseek-r1:free" })
        jsonBody.put("stream", true)

        val messagesArray = JSONArray()
        if (systemPrompt.isNotBlank()) {
            val systemObj = JSONObject()
            systemObj.put("role", "system")
            systemObj.put("content", systemPrompt)
            messagesArray.put(systemObj)
        }

        for (msg in messages) {
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            msgObj.put("content", msg.content)
            messagesArray.put(msgObj)
        }
        jsonBody.put("messages", messagesArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://ankigpt.app")
            .addHeader("X-Title", "ANKI GPT")

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "HTTP ${response.code}"
                throw Exception("API Error (${response.code}): $errorMsg")
            }

            val body = response.body ?: throw Exception("Empty response from API")
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        break
                    }
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
                    } catch (_: Exception) {
                        // ignore malformed SSE lines
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
