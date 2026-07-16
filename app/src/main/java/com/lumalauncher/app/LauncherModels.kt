package com.lumalauncher.app

import androidx.compose.ui.graphics.ImageBitmap

data class TvApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
    val isFromIconPack: Boolean = false,
)

data class IconPackInfo(
    val packageName: String,
    val title: String,
)

enum class BackgroundStyle(val title: String) {
    AURORA("Aurora"),
    MIDNIGHT("Midnight"),
    SUNSET("Sunset"),
    FOREST("Forest"),
    CUSTOM("My photo"),
}

enum class CardStyle(val title: String) {
    GLASS("Glass"),
    SOLID("Solid"),
    OUTLINE("Outline"),
    GLOW("Glow"),
}

enum class CardShapeStyle(val title: String) {
    ROUNDED("Rounded"),
    SOFT("Extra round"),
    SQUARE("Square"),
    CUT("Cut corners"),
}

enum class CardShade(val title: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    DARK("Dark"),
}

enum class CardColor(val title: String) {
    NEUTRAL("Neutral"),
    OCEAN("Ocean"),
    VIOLET("Violet"),
    TEAL("Teal"),
    EMBER("Ember"),
}

enum class CardSize(val title: String) {
    COMPACT("Compact"),
    COZY("Cozy"),
    LARGE("Large"),
}

enum class IconStyle(val title: String) {
    CLEAN("Clean large"),
    RAISED("Raised"),
    STICKER("Sticker"),
    NEON("Neon"),
    BUBBLE("Color bubble"),
}

data class WeatherLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val automatic: Boolean = false,
) {
    val key: String get() = if (automatic) "automatic" else "$latitude,$longitude"

    companion object {
        val automatic = WeatherLocation("Automatic location", 0.0, 0.0, automatic = true)
        val presets = listOf(
            WeatherLocation("Gulf Shores", 30.2460, -87.7008),
            WeatherLocation("Orange Beach", 30.2697, -87.5868),
            WeatherLocation("Mobile", 30.6954, -88.0399),
            WeatherLocation("Chicago", 41.8781, -87.6298),
            WeatherLocation("New York", 40.7128, -74.0060),
            WeatherLocation("Los Angeles", 34.0522, -118.2437),
            WeatherLocation("Dallas", 32.7767, -96.7970),
            WeatherLocation("Miami", 25.7617, -80.1918),
            WeatherLocation("Denver", 39.7392, -104.9903),
            WeatherLocation("Seattle", 47.6062, -122.3321),
            WeatherLocation("London", 51.5072, -0.1276),
            WeatherLocation("Sydney", -33.8688, 151.2093),
        )
    }
}

data class WeatherInfo(
    val locationName: String,
    val temperature: Int,
    val feelsLike: Int,
    val high: Int,
    val low: Int,
    val description: String,
    val symbol: String,
    val wind: Int,
)

data class ConnectionInfo(
    val type: String = "Checking",
    val address: String = "—",
    val connected: Boolean = false,
    val secureTunnel: Boolean = false,
)

data class ForecastDay(
    val date: String,
    val high: Int,
    val low: Int,
    val description: String,
    val symbol: String,
    val precipitationChance: Int,
)

data class WeatherForecast(
    val locationName: String,
    val days: List<ForecastDay>,
)

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
)

data class NowPlaying(
    val title: String,
    val artist: String,
    val appName: String,
    val isPlaying: Boolean,
    val albumArt: ImageBitmap? = null,
)
