package com.ankigpt.app

import com.ankigpt.app.data.AnkiCommandRouter
import com.ankigpt.app.data.ChatMessage
import com.ankigpt.app.data.CommandResult
import com.ankigpt.app.data.WebSearchService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUnitTest {

    @Test
    fun testChatMessageDataClass() {
        val msg = ChatMessage(role = "user", content = "Hello AnkiGPT")
        assertEquals("user", msg.role)
        assertEquals("Hello AnkiGPT", msg.content)
    }

    @Test
    fun testCommandRouterTimeLocalAction() {
        val router = AnkiCommandRouter(null, null)
        val result = router.processCommand("time")
        assertTrue(result is CommandResult.LocalAction)
        val local = result as CommandResult.LocalAction
        assertTrue(local.responseText.contains("It's "))
    }

    @Test
    fun testCommandRouterAiFallback() {
        val router = AnkiCommandRouter(null, null)
        val result = router.processCommand("help me debug this Kotlin code.")
        assertTrue(result is CommandResult.AiRequest)
    }

    @Test
    fun testWebSearchServiceFallback() = runBlocking {
        val webSearchService = WebSearchService()
        val query = "test query"
        val results = webSearchService.searchWeb(query)
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
    }
}
