package com.ankigpt.app.data.tts

import com.ankigpt.app.data.DeviceControlManager

class AndroidTtsProvider(private val deviceControlManager: DeviceControlManager) : TtsProvider {
    override fun providerName(): String = "Android Native TTS"

    override suspend fun synthesizeAndSpeak(text: String): Boolean {
        deviceControlManager.speakText(text)
        return true
    }

    override fun stop() {
        deviceControlManager.stopSpeaking()
    }

    override fun isAvailable(): Boolean = true
}
