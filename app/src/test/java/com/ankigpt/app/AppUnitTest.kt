package com.ankigpt.app

import com.ankigpt.app.data.ChatMessage
import com.ankigpt.app.data.RouterApiService
import com.ankigpt.app.data.WebSearchService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUnitTest {

    @Test
    fun testChatMessageDataClass() {
        val msg = ChatMessage(role = "user", content = "Hello ANKI GPT")
        assertEquals("user", msg.role)
        assertEquals("Hello ANKI GPT", msg.content)
    }

    @Test
    fun testWebSearchServiceFallback() = runBlocking {
        val webSearchService = WebSearchService()
        val query = "test query"
        val results = webSearchService.searchWeb(query)

        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].title.contains("test query", ignoreCase = true) || results[0].snippet.isNotEmpty())
    }
}
