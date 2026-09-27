package com.ankigpt.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anki_gpt_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val GONKA_API_KEY = stringPreferencesKey("gonka_api_key")
        val GONKA_BASE_URL = stringPreferencesKey("gonka_base_url")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")

        const val DEFAULT_BASE_URL = "https://api.openrouter.ai/v1"
        const val DEFAULT_MODEL = "deepseek/deepseek-r1:free"
        const val DEFAULT_SYSTEM_PROMPT = "You are ANKI GPT, an elite AI assistant equipped with advanced coding capabilities, file analysis, web search skills, and system hardware diagnostic controls."
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GONKA_API_KEY] ?: ""
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GONKA_BASE_URL] ?: DEFAULT_BASE_URL
    }

    val selectedModelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL] ?: DEFAULT_MODEL
    }

    val systemPromptFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[GONKA_API_KEY] = key
        }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GONKA_BASE_URL] = url
        }
    }

    suspend fun saveSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL] = model
        }
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[SYSTEM_PROMPT] = prompt
        }
    }
}
