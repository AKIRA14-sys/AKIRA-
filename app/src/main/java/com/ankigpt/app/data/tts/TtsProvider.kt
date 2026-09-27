package com.ankigpt.app.data.tts

interface TtsProvider {
    fun providerName(): String
    suspend fun synthesizeAndSpeak(text: String): Boolean
    fun stop()
    fun isAvailable(): Boolean
}
