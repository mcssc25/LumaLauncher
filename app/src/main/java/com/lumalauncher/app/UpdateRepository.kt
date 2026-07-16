package com.lumalauncher.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateRepository {
    const val STABLE_DOWNLOAD_URL =
        "https://github.com/mcssc25/LumaLauncher/releases/latest/download/Luma-Launcher.apk"

    suspend fun latest(currentVersion: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (
                URL("https://api.github.com/repos/mcssc25/LumaLauncher/releases/latest")
                    .openConnection() as HttpURLConnection
                ).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "LumaLauncher/$currentVersion")
            }
            try {
                check(connection.responseCode in 200..299) {
                    "Update service returned ${connection.responseCode}"
                }
                val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val latestVersion = root.getString("tag_name").removePrefix("v")
                if (compareVersions(latestVersion, currentVersion) <= 0) {
                    null
                } else {
                    val assets = root.getJSONArray("assets")
                    val downloadUrl = (0 until assets.length())
                        .map { assets.getJSONObject(it) }
                        .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                        ?.optString("browser_download_url")
                        ?.takeIf(String::isNotBlank)
                        ?: STABLE_DOWNLOAD_URL
                    UpdateInfo(latestVersion, downloadUrl)
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun compareVersions(first: String, second: String): Int {
        val firstParts = first.split('.').map { it.toIntOrNull() ?: 0 }
        val secondParts = second.split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(firstParts.size, secondParts.size)
        repeat(size) { index ->
            val difference = firstParts.getOrElse(index) { 0 } - secondParts.getOrElse(index) { 0 }
            if (difference != 0) return difference
        }
        return 0
    }
}
