package com.ankigpt.app.data.tts

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiTtsProvider(
    private val context: Context,
    private val apiKey: String,
    private val voiceName: String = "Puck"
) : TtsProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null

    override fun providerName(): String = "Gemini Cloud TTS"

    override suspend fun synthesizeAndSpeak(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/text-to-speech:generateSpeech?key=$apiKey"
            val json = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().put("name", voiceName))
                put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
            }
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bytes = response.body?.bytes() ?: return@withContext false

                val tempFile = File(context.cacheDir, "gemini_tts_audio.mp3")
                tempFile.writeBytes(bytes)

                withContext(Dispatchers.Main) {
                    stop()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        start()
                    }
                }
                return@withContext true
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isAvailable(): Boolean = apiKey.isNotBlank()
}
