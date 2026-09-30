package com.ankigpt.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CommandResult {
    data class LocalAction(val responseText: String, val isHandled: Boolean) : CommandResult()
    data class AiRequest(val prompt: String) : CommandResult()
}

class AnkiCommandRouter(
    private val appResolver: AnkiAppResolver?,
    private val deviceControlManager: DeviceControlManager?
) {

    fun processCommand(input: String): CommandResult {
        val cleanInput = input.trim()
        var lower = cleanInput.lowercase()

        // Recognized wake phrase prefixes: "hello anki", "hi anki", "hey anki", "a-anki", "a anki", "anki"
        val prefixes = listOf("hello anki", "hi anki", "hey anki", "a-anki", "a anki", "anki")
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                lower = lower.removePrefix(prefix)
                break
            }
        }
        lower = lower.replace(Regex("^[^a-z0-9]+"), "").replace(Regex("[?.!]+$"), "").trim()

        // 1. Time query
        if (lower == "what's the time" || lower == "what time is it" || lower == "time") {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return CommandResult.LocalAction("It's $timeStr.", true)
        }

        // 2. Date query
        if (lower == "what's today's date" || lower == "what date is it" || lower == "date" || lower == "today's date") {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            return CommandResult.LocalAction("Today is $dateStr.", true)
        }

        // 3. Battery query
        if (lower.contains("battery percentage") || lower.contains("battery level") || lower == "battery") {
            val status = deviceControlManager?.getSystemStatus()
            val pct = status?.batteryPercentage ?: 0
            val charging = if (status?.isCharging == true) "and charging" else "and not charging"
            return CommandResult.LocalAction("Your battery is at $pct% $charging.", true)
        }

        // 4. Open app command ("open whatsapp", "launch chrome", "start youtube")
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val targetApp = lower.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
            if (targetApp.isNotBlank() && !targetApp.startsWith("http")) {
                if (appResolver != null) {
                    val (success, msg) = appResolver.launchAppByName(targetApp)
                    return CommandResult.LocalAction(msg, success)
                }
            }
        }

        // 5. Google web search query
        if (lower.startsWith("search google for ") || lower.startsWith("google search ")) {
            val query = lower.removePrefix("search google for ").removePrefix("google search ").trim()
            if (appResolver != null) {
                val success = appResolver.searchGoogle(query)
                return CommandResult.LocalAction(
                    if (success) "Searching Google for \"$query\"..." else "Failed to launch web browser.",
                    success
                )
            }
        }

        // 6. Open website URL command
        if (lower.startsWith("open website ") || lower.startsWith("open url ") || lower.startsWith("http://") || lower.startsWith("https://")) {
            val url = lower.removePrefix("open website ").removePrefix("open url ").trim()
            if (appResolver != null) {
                val success = appResolver.openWebUrl(url)
                return CommandResult.LocalAction(
                    if (success) "Opening $url..." else "Unable to open website $url.",
                    success
                )
            }
        }

        // Fallback: Complex query requiring Cloud AI
        return CommandResult.AiRequest(cleanInput)
    }
}
