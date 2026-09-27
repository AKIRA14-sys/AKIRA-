package com.ankigpt.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class LocalFileInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean
)

class FileAccessService(private val context: Context) {

    suspend fun readFileContentFromUri(uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
        var fileName = "unknown_file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                var lineCount = 0
                while (reader.readLine().also { line = it } != null && lineCount < 2000) {
                    stringBuilder.append(line).append("\n")
                    lineCount++
                }
            }
        }

        return@withContext Pair(fileName, stringBuilder.toString())
    }

    suspend fun listFilesDirectory(dirPath: String): List<LocalFileInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocalFileInfo>()
        try {
            val targetDir = if (dirPath.isBlank()) context.getExternalFilesDir(null) else File(dirPath)
            if (targetDir != null && targetDir.exists() && targetDir.isDirectory) {
                val files = targetDir.listFiles() ?: emptyArray()
                for (file in files) {
                    results.add(
                        LocalFileInfo(
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            isDirectory = file.isDirectory
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext results
    }

    suspend fun readTextFileFromPath(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            return@withContext "Error: File does not exist or is not readable: $filePath"
        }

        val sb = StringBuilder()
        file.bufferedReader().use { reader ->
            var line: String?
            var count = 0
            while (reader.readLine().also { line = it } != null && count < 2000) {
                sb.append(line).append("\n")
                count++
            }
        }
        return@withContext sb.toString()
    }
}
