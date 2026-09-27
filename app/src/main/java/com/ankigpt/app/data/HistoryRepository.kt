package com.ankigpt.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage>,
    val timestamp: Long = System.currentTimeMillis()
)

class HistoryRepository(private val context: Context) {

    companion object {
        private val HISTORY_KEY = stringPreferencesKey("chat_conversations_json")
    }

    val conversationsFlow: Flow<List<ChatConversation>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[HISTORY_KEY] ?: "[]"
        parseConversations(jsonStr)
    }

    private fun parseConversations(jsonStr: String): List<ChatConversation> {
        val list = mutableListOf<ChatConversation>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val msgArray = obj.optJSONArray("messages") ?: JSONArray()
                val msgs = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val msgObj = msgArray.getJSONObject(j)
                    msgs.add(ChatMessage(msgObj.optString("role"), msgObj.optString("content")))
                }

                list.add(
                    ChatConversation(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "New Chat"),
                        messages = msgs,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    suspend fun saveConversation(conversation: ChatConversation) {
        context.dataStore.edit { preferences ->
            val list = parseConversations(preferences[HISTORY_KEY] ?: "[]").toMutableList()
            val existingIndex = list.indexOfFirst { it.id == conversation.id }
            if (existingIndex != -1) {
                list[existingIndex] = conversation
            } else {
                list.add(0, conversation)
            }

            val array = JSONArray()
            for (c in list) {
                val msgArray = JSONArray()
                for (m in c.messages) {
                    msgArray.put(JSONObject().apply {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
                array.put(JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("timestamp", c.timestamp)
                    put("messages", msgArray)
                })
            }
            preferences[HISTORY_KEY] = array.toString()
        }
    }

    suspend fun deleteConversation(id: String) {
        context.dataStore.edit { preferences ->
            val list = parseConversations(preferences[HISTORY_KEY] ?: "[]").filter { it.id != id }
            val array = JSONArray()
            for (c in list) {
                val msgArray = JSONArray()
                for (m in c.messages) {
                    msgArray.put(JSONObject().apply {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
                array.put(JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("timestamp", c.timestamp)
                    put("messages", msgArray)
                })
            }
            preferences[HISTORY_KEY] = array.toString()
        }
    }
}
