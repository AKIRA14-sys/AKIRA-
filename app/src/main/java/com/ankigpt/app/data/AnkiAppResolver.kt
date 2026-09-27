package com.ankigpt.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val launchIntent: Intent?
)

class AnkiAppResolver(private val context: Context) {

    private val appRegistry = mutableListOf<InstalledAppInfo>()
    private var isCacheInitialized = false

    fun refreshInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        appRegistry.clear()

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        for (resolveInfo in resolveInfos) {
            val label = resolveInfo.loadLabel(pm).toString().trim()
            val packageName = resolveInfo.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null && label.isNotBlank()) {
                appRegistry.add(InstalledAppInfo(label, packageName, launchIntent))
            }
        }
        isCacheInitialized = true
        return appRegistry
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        if (!isCacheInitialized || appRegistry.isEmpty()) {
            refreshInstalledApps()
        }
        return appRegistry
    }

    fun launchAppByName(appNameQuery: String): Pair<Boolean, String> {
        val apps = getInstalledApps()
        val query = appNameQuery.trim().lowercase()

        // Alias mapping
        val aliases = mapOf(
            "yt" to "youtube",
            "google chrome" to "chrome",
            "android settings" to "settings",
            "phone settings" to "settings"
        )
        val normalizedQuery = aliases[query] ?: query

        // Exact or contains match
        val matches = apps.filter {
            val labelLower = it.label.lowercase()
            labelLower == normalizedQuery || labelLower.contains(normalizedQuery) || normalizedQuery.contains(labelLower)
        }

        if (matches.size == 1) {
            val target = matches[0]
            return try {
                target.launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(target.launchIntent)
                Pair(true, "Opening ${target.label}...")
            } catch (e: Exception) {
                Pair(false, "Failed to launch ${target.label}: ${e.localizedMessage}")
            }
        } else if (matches.size > 1) {
            val matchNames = matches.take(3).joinToString(", ") { it.label }
            return Pair(false, "Multiple apps matched \"$appNameQuery\": $matchNames. Please specify which one you want.")
        }

        return Pair(false, "I couldn't find a launchable app called \"$appNameQuery\" installed on this device.")
    }

    fun openWebUrl(url: String): Boolean {
        return try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun searchGoogle(query: String): Boolean {
        return try {
            val encoded = Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
