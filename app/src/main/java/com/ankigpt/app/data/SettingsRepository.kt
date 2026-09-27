package com.ankigpt.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anki_gpt_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")

        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val GROQ_MODEL = stringPreferencesKey("groq_model")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")

        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val TTS_PROVIDER = stringPreferencesKey("tts_provider")
        val ELEVENLABS_API_KEY = stringPreferencesKey("elevenlabs_api_key")
        val ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")

        const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_SYSTEM_PROMPT = "You are AnkiGPT, an elite private personal AI assistant equipped with advanced coding capabilities, file analysis, web search skills, and system hardware diagnostic controls."
        const val DEFAULT_TTS_PROVIDER = "android"
    }

    val selectedProviderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_PROVIDER] ?: "OPENROUTER"
    }

    val openRouterApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OPENROUTER_API_KEY] ?: ""
    }

    val groqApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GROQ_API_KEY] ?: ""
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: ""
    }

    val openRouterModelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OPENROUTER_MODEL] ?: "deepseek/deepseek-r1:free"
    }

    val groqModelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GROQ_MODEL] ?: "llama-3.3-70b-versatile"
    }

    val geminiModelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_MODEL] ?: "gemini-1.5-flash"
    }

    val systemPromptFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT
    }

    val ttsProviderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TTS_PROVIDER] ?: DEFAULT_TTS_PROVIDER
    }

    val elevenLabsApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ELEVENLABS_API_KEY] ?: ""
    }

    val elevenLabsVoiceIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ELEVENLABS_VOICE_ID] ?: "21m00Tcm4TlvDq8ikWAM"
    }

    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[APP_LOCK_ENABLED] ?: false
    }

    suspend fun saveSelectedProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PROVIDER] = provider
        }
    }

    suspend fun saveOpenRouterApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[OPENROUTER_API_KEY] = key
        }
    }

    suspend fun saveGroqApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[GROQ_API_KEY] = key
        }
    }

    suspend fun saveGeminiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = key
        }
    }

    suspend fun saveOpenRouterModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[OPENROUTER_MODEL] = model
        }
    }

    suspend fun saveGroqModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[GROQ_MODEL] = model
        }
    }

    suspend fun saveGeminiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_MODEL] = model
        }
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun saveTtsProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[TTS_PROVIDER] = provider
        }
    }

    suspend fun saveElevenLabsApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[ELEVENLABS_API_KEY] = key
        }
    }

    suspend fun saveElevenLabsVoiceId(voiceId: String) {
        context.dataStore.edit { preferences ->
            preferences[ELEVENLABS_VOICE_ID] = voiceId
        }
    }

    suspend fun saveAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }
}
