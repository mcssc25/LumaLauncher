package com.lumalauncher.app

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

object WeatherRepository {
    suspend fun current(
        location: WeatherLocation,
        fahrenheit: Boolean,
    ): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val resolvedLocation = if (location.automatic) approximateInternetLocation() else location
            val temperatureUnit = if (fahrenheit) "fahrenheit" else "celsius"
            val windUnit = if (fahrenheit) "mph" else "kmh"
            val endpoint = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=${resolvedLocation.latitude}&longitude=${resolvedLocation.longitude}")
                append("&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m")
                append("&daily=temperature_2m_max,temperature_2m_min")
                append("&temperature_unit=$temperatureUnit&wind_speed_unit=$windUnit")
                append("&forecast_days=1&timezone=auto")
            }
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LumaLauncher/0.1")
            }
            try {
                check(connection.responseCode in 200..299) { "Weather service returned ${connection.responseCode}" }
                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(payload)
                val now = root.getJSONObject("current")
                val daily = root.getJSONObject("daily")
                val code = now.getInt("weather_code")
                val condition = weatherCondition(code)
                WeatherInfo(
                    locationName = resolvedLocation.name,
                    temperature = now.getDouble("temperature_2m").roundToInt(),
                    feelsLike = now.getDouble("apparent_temperature").roundToInt(),
                    high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                    low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                    description = condition.first,
                    symbol = condition.second,
                    wind = now.getDouble("wind_speed_10m").roundToInt(),
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun forecast(
        location: WeatherLocation,
        fahrenheit: Boolean,
    ): Result<WeatherForecast> = withContext(Dispatchers.IO) {
        runCatching {
            val resolvedLocation = if (location.automatic) approximateInternetLocation() else location
            val temperatureUnit = if (fahrenheit) "fahrenheit" else "celsius"
            val endpoint = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=${resolvedLocation.latitude}&longitude=${resolvedLocation.longitude}")
                append("&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                append("&temperature_unit=$temperatureUnit&forecast_days=7&timezone=auto")
            }
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LumaLauncher/0.2")
            }
            try {
                check(connection.responseCode in 200..299) { "Weather service returned ${connection.responseCode}" }
                val daily = JSONObject(
                    connection.inputStream.bufferedReader().use { it.readText() },
                ).getJSONObject("daily")
                val dates = daily.getJSONArray("time")
                val codes = daily.getJSONArray("weather_code")
                val highs = daily.getJSONArray("temperature_2m_max")
                val lows = daily.getJSONArray("temperature_2m_min")
                val precipitation = daily.getJSONArray("precipitation_probability_max")
                val days = (0 until dates.length()).map { index ->
                    val condition = weatherCondition(codes.getInt(index))
                    ForecastDay(
                        date = dates.getString(index),
                        high = highs.getDouble(index).roundToInt(),
                        low = lows.getDouble(index).roundToInt(),
                        description = condition.first,
                        symbol = condition.second,
                        precipitationChance = precipitation.optInt(index, 0),
                    )
                }
                WeatherForecast(resolvedLocation.name, days)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun approximateInternetLocation(): WeatherLocation {
        val connection = (URL("https://ipwho.is/").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LumaLauncher/0.1")
        }
        try {
            check(connection.responseCode in 200..299) { "Location service returned ${connection.responseCode}" }
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            check(root.optBoolean("success", true)) { "Location could not be determined" }
            val city = root.optString("city").ifBlank { "Local weather" }
            return WeatherLocation(
                name = city,
                latitude = root.getDouble("latitude"),
                longitude = root.getDouble("longitude"),
            )
        } finally {
            connection.disconnect()
        }
    }

    internal fun weatherCondition(code: Int): Pair<String, String> = when (code) {
        0 -> "Clear" to "☀"
        1 -> "Mostly clear" to "🌤"
        2 -> "Partly cloudy" to "⛅"
        3 -> "Cloudy" to "☁"
        45, 48 -> "Foggy" to "≋"
        51, 53, 55, 56, 57 -> "Drizzle" to "☂"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain" to "☂"
        71, 73, 75, 77, 85, 86 -> "Snow" to "❄"
        95, 96, 99 -> "Thunderstorms" to "ϟ"
        else -> "Weather" to "◌"
    }
}

object NetworkReader {
    fun read(context: Context): ConnectionInfo {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return ConnectionInfo(type = "Offline")
        val capabilities = manager.getNetworkCapabilities(network) ?: return ConnectionInfo(type = "Offline")
        val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val secureTunnel = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val type = when {
            secureTunnel -> "VPS / VPN"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            else -> "Connected"
        }
        val address = manager.getLinkProperties(network)
            ?.linkAddresses
            ?.firstOrNull { it.address.hostAddress?.contains(':') == false }
            ?.address
            ?.hostAddress
            ?: "No local IP"
        return ConnectionInfo(
            type = type,
            address = address,
            connected = connected,
            secureTunnel = secureTunnel,
        )
    }
}

object MusicReader {
    fun hasAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.contains(context.packageName)
    }

    fun read(context: Context): NowPlaying? {
        if (!hasAccess(context)) return null
        return runCatching {
            val controller = preferredController(context) ?: return null
            val metadata = controller.metadata
            val playbackState = controller.playbackState
            val title = metadata?.string(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.string(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "Nothing playing"
            val artist = metadata?.string(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.string(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: "Open a music app"
            val artwork = metadata?.bitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.bitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.bitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            val appName = runCatching {
                val info = context.packageManager.getApplicationInfo(controller.packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(controller.packageName)
            NowPlaying(
                title = title,
                artist = artist,
                appName = appName,
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                canSkipNext = playbackState?.actions?.and(PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
                albumArt = artwork?.safeImageBitmap(),
            )
        }.getOrNull()
    }

    fun togglePlayback(context: Context): Boolean {
        if (!hasAccess(context)) return false
        val controller = runCatching { preferredController(context) }.getOrNull() ?: return false
        return runCatching {
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        }.getOrDefault(false)
    }

    fun play(context: Context, preferredPackageName: String? = null): Boolean {
        if (!hasAccess(context)) return false
        val controller = runCatching {
            preferredController(context, preferredPackageName)
        }.getOrNull() ?: return false
        val controllerAccepted = runCatching {
            controller.transportControls.play()
            true
        }.getOrDefault(false)
        val mediaKeyAccepted = runCatching {
            val audioManager = context.getSystemService(AudioManager::class.java)
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
            true
        }.getOrDefault(false)
        return controllerAccepted || mediaKeyAccepted
    }

    fun isPlaying(context: Context, preferredPackageName: String? = null): Boolean {
        if (!hasAccess(context)) return false
        return runCatching {
            preferredController(context, preferredPackageName)?.playbackState?.state == PlaybackState.STATE_PLAYING
        }.getOrDefault(false)
    }

    fun skipToNext(context: Context): Boolean {
        if (!hasAccess(context)) return false
        val controller = runCatching { preferredController(context) }.getOrNull() ?: return false
        return runCatching {
            controller.transportControls.skipToNext()
            true
        }.getOrDefault(false)
    }

    private fun activeControllers(context: Context): List<MediaController> {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, MusicNotificationListener::class.java)
        return manager.getActiveSessions(listener)
    }

    private fun preferredController(
        context: Context,
        preferredPackageName: String? = null,
    ): MediaController? {
        val controllers = activeControllers(context)
        return preferredPackageName?.let { packageName ->
            controllers.firstOrNull { it.packageName == packageName }
        } ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        ?: controllers.firstOrNull()
    }

    private fun MediaMetadata.string(key: String): String? = getString(key)?.takeIf(String::isNotBlank)

    private fun MediaMetadata.bitmap(key: String): Bitmap? = getBitmap(key)

    private fun Bitmap.safeImageBitmap() = runCatching { asImageBitmap() }.getOrNull()
}
