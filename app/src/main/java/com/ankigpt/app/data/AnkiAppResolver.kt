package com.ankigpt.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

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
        val foundPackages = mutableSetOf<String>()

        val queryFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }

        // 1. Query launcher intent activities
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        try {
            val resolveInfos = pm.queryIntentActivities(mainIntent, queryFlags)
            for (resolveInfo in resolveInfos) {
                val label = resolveInfo.loadLabel(pm).toString().trim()
                val packageName = resolveInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null && label.isNotBlank() && !foundPackages.contains(packageName)) {
                    foundPackages.add(packageName)
                    appRegistry.add(InstalledAppInfo(label, packageName, launchIntent))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback query for all installed packages
        try {
            val pkgFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL or PackageManager.GET_META_DATA
            } else {
                PackageManager.GET_META_DATA
            }
            val installedPackages = pm.getInstalledPackages(pkgFlags)
            for (pkg in installedPackages) {
                val pkgName = pkg.packageName
                if (!foundPackages.contains(pkgName)) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                    if (launchIntent != null) {
                        val appInfo = pkg.applicationInfo
                        val label = if (appInfo != null) appInfo.loadLabel(pm).toString().trim() else pkgName
                        if (label.isNotBlank()) {
                            foundPackages.add(pkgName)
                            appRegistry.add(InstalledAppInfo(label, pkgName, launchIntent))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

        // Common Aliases Mapping
        val aliases = mapOf(
            "yt" to "youtube",
            "google chrome" to "chrome",
            "android settings" to "settings",
            "phone settings" to "settings",
            "whats app" to "whatsapp",
            "wa" to "whatsapp"
        )
        val normalizedQuery = aliases[query] ?: query

        // Exact match on label or package name
        var matches = apps.filter {
            it.label.lowercase() == normalizedQuery || it.packageName.lowercase().contains(normalizedQuery)
        }

        // Substring match on label
        if (matches.isEmpty()) {
            matches = apps.filter {
                it.label.lowercase().contains(normalizedQuery) || normalizedQuery.contains(it.label.lowercase())
            }
        }

        if (matches.isNotEmpty()) {
            val target = matches[0]
            return try {
                val intent = pmLaunchIntent(target.packageName) ?: target.launchIntent
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                    Pair(true, "Opening ${target.label}...")
                } else {
                    Pair(false, "Could not obtain launch intent for ${target.label}.")
                }
            } catch (e: Exception) {
                Pair(false, "Failed to launch ${target.label}: ${e.localizedMessage}")
            }
        }

        return Pair(false, "I couldn't find a launchable app called \"$appNameQuery\" installed on this device.")
    }

    private fun pmLaunchIntent(packageName: String): Intent? {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            null
        }
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
