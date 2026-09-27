package com.ankigpt.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class AnkiAppResolver(private val context: Context) {

    fun launchAppByName(appNameQuery: String): Pair<Boolean, String> {
        val pm = context.packageManager
        val query = appNameQuery.trim().lowercase()

        // Direct package mapping for common Android apps
        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "youtube" to "com.google.android.youtube",
            "google" to "com.google.android.googlequicksearchbox",
            "settings" to "com.android.settings",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm"
        )

        val directPkg = knownPackages[query]
        if (directPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return Pair(true, "Opening ${appNameQuery.replaceFirstChar { it.uppercase() }}...")
            }
        }

        // Query installed applications dynamically
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        for (resolveInfo in resolveInfos) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                val pkgName = resolveInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Pair(true, "Opening ${resolveInfo.loadLabel(pm)}...")
                }
            }
        }

        return Pair(false, "I couldn't find \"$appNameQuery\" installed on this phone.")
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
