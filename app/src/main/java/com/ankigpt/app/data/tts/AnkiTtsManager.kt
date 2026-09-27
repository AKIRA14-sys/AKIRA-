package com.ankigpt.app.data.tts

import android.content.Context
import com.ankigpt.app.data.DeviceControlManager

class AnkiTtsManager(
    private val context: Context,
    private val deviceControlManager: DeviceControlManager
) {

    private val androidFallback = AndroidTtsProvider(deviceControlManager)

    suspend fun speak(
        text: String,
        selectedProviderType: String,
        geminiApiKey: String,
        elevenLabsApiKey: String,
        elevenLabsVoiceId: String
    ) {
        var spokeSuccessfully = false

        val provider: TtsProvider? = when (selectedProviderType.lowercase()) {
            "gemini" -> GeminiTtsProvider(context, geminiApiKey)
            "elevenlabs" -> ElevenLabsTtsProvider(context, elevenLabsApiKey, elevenLabsVoiceId)
            else -> androidFallback
        }

        if (provider != null && provider.isAvailable()) {
            spokeSuccessfully = provider.synthesizeAndSpeak(text)
        }

        // Automatic Fallback to Android Native TTS if cloud provider failed or was unavailable
        if (!spokeSuccessfully) {
            androidFallback.synthesizeAndSpeak(text)
        }
    }

    fun stop() {
        androidFallback.stop()
    }
}
