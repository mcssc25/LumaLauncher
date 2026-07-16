package com.lumalauncher.app

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LauncherPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("luma_launcher", Context.MODE_PRIVATE)

    var backgroundStyle by mutableStateOf(
        enumValueOrDefault(prefs.getString("background", null), BackgroundStyle.AURORA),
    )
        private set

    var cardStyle by mutableStateOf(
        enumValueOrDefault(prefs.getString("card_style", null), CardStyle.GLASS),
    )
        private set

    var cardSize by mutableStateOf(
        enumValueOrDefault(prefs.getString("card_size", null), CardSize.COZY),
    )
        private set

    var cardShape by mutableStateOf(
        enumValueOrDefault(prefs.getString("card_shape", null), CardShapeStyle.ROUNDED),
    )
        private set

    var cardShade by mutableStateOf(
        enumValueOrDefault(prefs.getString("card_shade", null), CardShade.DARK),
    )
        private set

    var cardColor by mutableStateOf(
        enumValueOrDefault(prefs.getString("card_color", null), CardColor.NEUTRAL),
    )
        private set

    var iconStyle by mutableStateOf(
        enumValueOrDefault(prefs.getString("icon_style", null), IconStyle.CLEAN),
    )
        private set

    var iconPackPackage by mutableStateOf(prefs.getString("icon_pack_package", null))
        private set

    var customBackgroundUri by mutableStateOf(
        prefs.getString("custom_background_uri", null)?.let(Uri::parse),
    )
        private set

    var weatherLocation by mutableStateOf(loadWeatherLocation())
        private set

    var useFahrenheit by mutableStateOf(prefs.getBoolean("weather_fahrenheit", true))
        private set

    val favoritePackages = mutableStateListOf<String>().apply {
        addAll(
            prefs.getString("favorites", "")
                .orEmpty()
                .split(FAVORITES_SEPARATOR)
                .filter(String::isNotBlank),
        )
    }

    val customIconUris = mutableStateMapOf<String, String>().apply {
        prefs.getStringSet("custom_icons", emptySet()).orEmpty().forEach { entry ->
            val separator = entry.indexOf(CUSTOM_ICON_SEPARATOR)
            if (separator > 0) put(entry.substring(0, separator), entry.substring(separator + 1))
        }
    }

    fun setBackground(style: BackgroundStyle) {
        backgroundStyle = style
        prefs.edit().putString("background", style.name).apply()
    }

    fun setCustomBackground(uri: Uri) {
        customBackgroundUri = uri
        backgroundStyle = BackgroundStyle.CUSTOM
        prefs.edit()
            .putString("custom_background_uri", uri.toString())
            .putString("background", BackgroundStyle.CUSTOM.name)
            .apply()
    }

    fun updateCardStyle(style: CardStyle) {
        cardStyle = style
        prefs.edit().putString("card_style", style.name).apply()
    }

    fun updateCardSize(size: CardSize) {
        cardSize = size
        prefs.edit().putString("card_size", size.name).apply()
    }

    fun updateCardShape(shape: CardShapeStyle) {
        cardShape = shape
        prefs.edit().putString("card_shape", shape.name).apply()
    }

    fun updateCardShade(shade: CardShade) {
        cardShade = shade
        prefs.edit().putString("card_shade", shade.name).apply()
    }

    fun updateCardColor(color: CardColor) {
        cardColor = color
        prefs.edit().putString("card_color", color.name).apply()
    }

    fun updateIconStyle(style: IconStyle) {
        iconStyle = style
        prefs.edit().putString("icon_style", style.name).apply()
    }

    fun updateIconPack(packageName: String?) {
        iconPackPackage = packageName
        prefs.edit().apply {
            if (packageName == null) remove("icon_pack_package") else putString("icon_pack_package", packageName)
        }.apply()
    }

    fun updateWeatherLocation(location: WeatherLocation) {
        weatherLocation = location
        prefs.edit().apply {
            putBoolean("weather_automatic", location.automatic)
            if (location.automatic) {
                remove("weather_name")
                remove("weather_latitude")
                remove("weather_longitude")
            } else {
                putString("weather_name", location.name)
                putString("weather_latitude", location.latitude.toString())
                putString("weather_longitude", location.longitude.toString())
            }
        }.apply()
    }

    fun setFahrenheit(enabled: Boolean) {
        useFahrenheit = enabled
        prefs.edit().putBoolean("weather_fahrenheit", enabled).apply()
    }

    fun toggleFavorite(packageName: String) {
        if (favoritePackages.contains(packageName)) {
            favoritePackages.remove(packageName)
        } else {
            favoritePackages.add(packageName)
        }
        saveFavorites()
    }

    fun moveFavorite(packageName: String, delta: Int) {
        val current = favoritePackages.indexOf(packageName)
        if (current == -1) return
        val target = (current + delta).coerceIn(0, favoritePackages.lastIndex)
        if (target == current) return
        favoritePackages.removeAt(current)
        favoritePackages.add(target, packageName)
        saveFavorites()
    }

    fun setCustomIcon(packageName: String, uri: Uri) {
        customIconUris[packageName] = uri.toString()
        saveCustomIcons()
    }

    fun removeCustomIcon(packageName: String) {
        customIconUris.remove(packageName)
        saveCustomIcons()
    }

    private fun saveFavorites() {
        prefs.edit().putString("favorites", favoritePackages.joinToString(FAVORITES_SEPARATOR)).apply()
    }

    private fun saveCustomIcons() {
        val entries = customIconUris.mapTo(mutableSetOf()) { (packageName, uri) ->
            "$packageName$CUSTOM_ICON_SEPARATOR$uri"
        }
        prefs.edit().putStringSet("custom_icons", entries).apply()
    }

    private fun loadWeatherLocation(): WeatherLocation {
        val hasSavedCoordinates = prefs.contains("weather_latitude") && prefs.contains("weather_longitude")
        if (prefs.getBoolean("weather_automatic", !hasSavedCoordinates)) return WeatherLocation.automatic
        val fallback = WeatherLocation.presets.first()
        val name = prefs.getString("weather_name", null) ?: fallback.name
        val latitude = prefs.getString("weather_latitude", null)?.toDoubleOrNull() ?: fallback.latitude
        val longitude = prefs.getString("weather_longitude", null)?.toDoubleOrNull() ?: fallback.longitude
        return WeatherLocation(name, latitude, longitude)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private companion object {
        const val FAVORITES_SEPARATOR = "|"
        const val CUSTOM_ICON_SEPARATOR = '\t'
    }
}
