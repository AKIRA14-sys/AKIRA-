package com.ankigpt.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserMemory(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MemoryRepository(private val context: Context) {

    companion object {
        private val MEMORIES_KEY = stringPreferencesKey("user_memories_json")
    }

    val memoriesFlow: Flow<List<UserMemory>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[MEMORIES_KEY] ?: "[]"
        parseMemories(jsonStr)
    }

    private fun parseMemories(jsonStr: String): List<UserMemory> {
        val list = mutableListOf<UserMemory>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UserMemory(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        content = obj.optString("content", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    suspend fun addMemory(content: String) {
        if (content.isBlank()) return
        context.dataStore.edit { preferences ->
            val currentList = parseMemories(preferences[MEMORIES_KEY] ?: "[]").toMutableList()
            currentList.add(0, UserMemory(content = content.trim()))

            val array = JSONArray()
            for (item in currentList) {
                array.put(JSONObject().apply {
                    put("id", item.id)
                    put("content", item.content)
                    put("timestamp", item.timestamp)
                })
            }
            preferences[MEMORIES_KEY] = array.toString()
        }
    }

    suspend fun deleteMemory(id: String) {
        context.dataStore.edit { preferences ->
            val currentList = parseMemories(preferences[MEMORIES_KEY] ?: "[]").filter { it.id != id }
            val array = JSONArray()
            for (item in currentList) {
                array.put(JSONObject().apply {
                    put("id", item.id)
                    put("content", item.content)
                    put("timestamp", item.timestamp)
                })
            }
            preferences[MEMORIES_KEY] = array.toString()
        }
    }

    suspend fun clearAllMemories() {
        context.dataStore.edit { preferences ->
            preferences[MEMORIES_KEY] = "[]"
        }
    }
}
