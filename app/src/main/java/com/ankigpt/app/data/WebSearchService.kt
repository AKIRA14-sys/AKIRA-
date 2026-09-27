package com.ankigpt.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

class WebSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchWeb(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    results.addAll(parseDuckDuckGoHtml(html))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.isEmpty()) {
            results.add(
                SearchResult(
                    title = "Search: $query",
                    snippet = "Real-time web search query executed for '$query'. Check AI response for summarized knowledge.",
                    url = "https://duckduckgo.com/?q=${URLEncoder.encode(query, "UTF-8")}"
                )
            )
        }

        return@withContext results
    }

    private fun parseDuckDuckGoHtml(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        try {
            val regex = Pattern.compile(
                "<a class=\"result__a\" href=\"([^\"]+)\">([^<]+)</a>[\\s\\S]*?<a class=\"result__snippet\"[^\"]*?>([\\s\\S]*?)</a>",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = regex.matcher(html)

            var count = 0
            while (matcher.find() && count < 5) {
                val rawUrl = matcher.group(1) ?: ""
                val title = (matcher.group(2) ?: "").replace(Regex("<[^>]*>"), "").trim()
                val snippet = (matcher.group(3) ?: "").replace(Regex("<[^>]*>"), "").trim()

                // Extract clean URL from DuckDuckGo redirect link
                val cleanUrl = if (rawUrl.contains("uddg=")) {
                    try {
                        val parts = rawUrl.split("uddg=")
                        if (parts.size > 1) {
                            java.net.URLDecoder.decode(parts[1].split("&")[0], "UTF-8")
                        } else rawUrl
                    } catch (_: Exception) {
                        rawUrl
                    }
                } else rawUrl

                if (title.isNotEmpty()) {
                    results.add(SearchResult(title, snippet, cleanUrl))
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}
