package com.lumalauncher.app

import android.content.Context
import android.content.Intent
import android.app.role.RoleManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image as ImageIcon
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        setContent {
            MaterialTheme {
                LumaLauncher()
            }
        }
    }
}

@Composable
private fun LumaLauncher() {
    val context = LocalContext.current
    val preferences = remember { LauncherPreferences(context) }
    var showSettings by remember { mutableStateOf(false) }
    var showForecast by remember { mutableStateOf(false) }
    val customIconUris = preferences.customIconUris.toMap()
    val apps by produceState<List<TvApp>>(emptyList(), preferences.iconPackPackage, customIconUris) {
        val discovered = AppRepository.loadLaunchableApps(context)
        val themed = preferences.iconPackPackage?.let { iconPack ->
            IconPackRepository.apply(context, discovered, iconPack)
        } ?: discovered
        value = CustomIconRepository.apply(context, themed, customIconUris)
    }
    val updateInfo by produceState<UpdateInfo?>(null) {
        if (!BuildConfig.IS_PLAY_STORE_BUILD) {
            while (true) {
                value = UpdateRepository.latest(BuildConfig.VERSION_NAME).getOrNull()
                delay(6 * 60 * 60 * 1_000L)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LauncherBackground(preferences)
        if (showForecast) {
            ForecastScreen(
                preferences = preferences,
                onClose = { showForecast = false },
            )
        } else if (showSettings) {
            SettingsScreen(
                apps = apps,
                preferences = preferences,
                updateInfo = updateInfo,
                onClose = { showSettings = false },
            )
        } else {
            HomeContent(
                apps = apps,
                preferences = preferences,
                onSettings = { showSettings = true },
                onAndroidSettings = { openAndroidSettings(context) },
                onWeather = { showForecast = true },
                updateInfo = updateInfo,
                onUpdate = {
                    if (BuildConfig.IS_PLAY_STORE_BUILD) openPlayStorePage(context)
                    else openWebUrl(context, updateInfo?.downloadUrl ?: UpdateRepository.STABLE_DOWNLOAD_URL)
                },
            )
        }
    }
}

@Composable
private fun LauncherBackground(preferences: LauncherPreferences) {
    val context = LocalContext.current
    var customImage by remember(preferences.customBackgroundUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(preferences.customBackgroundUri) {
        customImage = preferences.customBackgroundUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    decodeWallpaper(context, uri)
                }.getOrNull()
            }
        }
    }

    val gradient = when (preferences.backgroundStyle) {
        BackgroundStyle.AURORA -> Brush.linearGradient(
            listOf(Color(0xFF07111F), Color(0xFF16304A), Color(0xFF321D48)),
        )
        BackgroundStyle.MIDNIGHT -> Brush.radialGradient(
            listOf(Color(0xFF25314D), Color(0xFF080B13)),
        )
        BackgroundStyle.SUNSET -> Brush.linearGradient(
            listOf(Color(0xFF3C193C), Color(0xFFB24E52), Color(0xFF18243C)),
        )
        BackgroundStyle.FOREST -> Brush.linearGradient(
            listOf(Color(0xFF071B17), Color(0xFF17483C), Color(0xFF182A33)),
        )
        BackgroundStyle.CUSTOM -> Brush.linearGradient(
            listOf(Color(0xFF111827), Color(0xFF06080D)),
        )
    }

    Box(Modifier.fillMaxSize().background(gradient)) {
        if (preferences.backgroundStyle == BackgroundStyle.CUSTOM && customImage != null) {
            Image(
                bitmap = customImage!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.72f)),
                    ),
                ),
            )
        } else {
            Box(
                Modifier
                    .size(700.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x5572D5FF), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x7704070C))),
            ),
        )
    }
}

private fun decodeWallpaper(context: Context, uri: android.net.Uri): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val ratio = max(bounds.outWidth / 1920f, bounds.outHeight / 1080f)
    var sampleSize = 1
    while (sampleSize * 2 <= ratio) sampleSize *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
    }
}

@Composable
private fun HomeContent(
    apps: List<TvApp>,
    preferences: LauncherPreferences,
    onSettings: () -> Unit,
    onAndroidSettings: () -> Unit,
    onWeather: () -> Unit,
    updateInfo: UpdateInfo?,
    onUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val favorites = preferences.favoritePackages.mapNotNull { packageName ->
        apps.firstOrNull { it.packageName == packageName }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 58.dp,
            end = 58.dp,
            top = 34.dp,
            bottom = 48.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { LauncherHeader(onSettings, onAndroidSettings, updateInfo, onUpdate) }
        item { WidgetRow(preferences, onWeather) }
        item {
            SectionTitle(icon = Icons.Rounded.Star, title = "Favorites", hint = "Hold OK to add or remove")
        }
        item {
            if (favorites.isEmpty()) {
                EmptyFavorites()
            } else {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowItemsIndexed(favorites, key = { _, app -> app.packageName }) { index, app ->
                        AppCard(
                            app = app,
                            preferences = preferences,
                            onClick = { AppRepository.launch(context, app) },
                            onLongClick = {
                                preferences.toggleFavorite(app.packageName)
                                Toast.makeText(context, "Removed ${app.label} from favorites", Toast.LENGTH_SHORT).show()
                            },
                            requestInitialFocus = index == 0,
                        )
                    }
                }
            }
        }
        item {
            SectionTitle(icon = Icons.Rounded.Apps, title = "All apps", hint = "Your installed TV apps")
        }
        item {
            if (apps.isEmpty()) {
                Text("Finding your apps…", color = Color.White.copy(alpha = 0.65f), fontSize = 18.sp)
            } else {
                val dimensions = cardDimensions(preferences.cardSize)
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.height * 2 + 36.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    gridItemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        val isFavorite = preferences.favoritePackages.contains(app.packageName)
                        AppCard(
                            app = app,
                            preferences = preferences,
                            favorite = isFavorite,
                            onClick = { AppRepository.launch(context, app) },
                            onLongClick = {
                                preferences.toggleFavorite(app.packageName)
                                val message = if (isFavorite) {
                                    "Removed ${app.label} from favorites"
                                } else {
                                    "Added ${app.label} to favorites"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            requestInitialFocus = favorites.isEmpty() && index == 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherHeader(
    onCustomize: () -> Unit,
    onAndroidSettings: () -> Unit,
    updateInfo: UpdateInfo?,
    onUpdate: () -> Unit,
) {
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(1_000)
        }
    }
    val time = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(time, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Text(date, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
        }
        Spacer(Modifier.width(22.dp))
        if (updateInfo != null) {
            FocusButton(
                label = "Update ${updateInfo.versionName}",
                icon = Icons.Rounded.SystemUpdateAlt,
                onClick = onUpdate,
            )
            Spacer(Modifier.width(12.dp))
        }
        FocusButton(label = "Customize", icon = Icons.Rounded.Palette, onClick = onCustomize)
        Spacer(Modifier.width(12.dp))
        FocusButton(label = "Android settings", icon = Icons.Rounded.Settings, onClick = onAndroidSettings)
    }
}

private fun openAndroidSettings(context: Context) {
    val opened = runCatching {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }.isSuccess
    if (!opened) {
        runCatching { context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
    }
}

private fun openDefaultHomeChooser(context: Context) {
    val roleIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        } else {
            null
        }
    } else {
        null
    }
    val opened = runCatching {
        context.startActivity(roleIntent ?: Intent(Settings.ACTION_HOME_SETTINGS))
    }.isSuccess
    if (!opened) openAndroidSettings(context)
}

private fun openAccessibilitySettings(context: Context) {
    val opened = runCatching {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }.isSuccess
    if (!opened) openAndroidSettings(context)
}

private fun openWebUrl(context: Context, url: String) {
    val opened = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.isSuccess
    if (!opened) {
        Toast.makeText(context, "No browser or Downloader app was found", Toast.LENGTH_LONG).show()
    }
}

private fun openPlayStorePage(context: Context) {
    val marketUri = Uri.parse("market://details?id=${context.packageName}")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
    val opened = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
    }.isSuccess
    if (!opened) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, webUri)) }
}

@Composable
private fun WidgetRow(preferences: LauncherPreferences, onWeather: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(126.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WeatherWidget(preferences, onWeather, Modifier.weight(0.92f).fillMaxHeight())
        NetworkWidget(Modifier.weight(0.86f).fillMaxHeight())
        MusicWidget(Modifier.weight(1.35f).fillMaxHeight())
    }
}

private data class WeatherLoad(val info: WeatherInfo? = null, val failed: Boolean = false)

@Composable
private fun WeatherWidget(
    preferences: LauncherPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by produceState(
        initialValue = WeatherLoad(),
        preferences.weatherLocation.key,
        preferences.useFahrenheit,
    ) {
        while (true) {
            val result = WeatherRepository.current(preferences.weatherLocation, preferences.useFahrenheit)
            value = result.fold(
                onSuccess = { WeatherLoad(info = it) },
                onFailure = { WeatherLoad(failed = true) },
            )
            delay(30 * 60 * 1_000L)
        }
    }
    WidgetShell(modifier, onClick = onClick) {
        val unit = if (preferences.useFahrenheit) "°F" else "°C"
        val info = state.info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(info?.symbol ?: "◌", color = Color(0xFFFFD784), fontSize = 38.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = info?.let { "${it.temperature}$unit" } ?: if (state.failed) "Unavailable" else "Loading…",
                    color = Color.White,
                    fontSize = if (info == null) 17.sp else 29.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = info?.description ?: if (preferences.weatherLocation.automatic) "Finding your city" else preferences.weatherLocation.name,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = info?.let { "${it.locationName}  ·  H ${it.high}°  L ${it.low}°" }
                ?: if (preferences.weatherLocation.automatic) "Weather · Automatic location" else "Weather · ${preferences.weatherLocation.name}",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun NetworkWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val info by produceState(ConnectionInfo()) {
        while (true) {
            value = NetworkReader.read(context)
            delay(5_000)
        }
    }
    WidgetShell(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LumaIcon(Icons.Rounded.NetworkCheck, "Network", infoColor(info.connected), 30.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(info.type, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        info.secureTunnel -> "Secure tunnel connected"
                        info.connected -> "Internet connected"
                        else -> "No internet"
                    },
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(info.address, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val access by produceState(MusicReader.hasAccess(context)) {
        while (true) {
            value = MusicReader.hasAccess(context)
            delay(2_000)
        }
    }
    val nowPlaying by produceState<NowPlaying?>(null, access) {
        while (true) {
            value = if (access) MusicReader.read(context) else null
            delay(2_000)
        }
    }
    val border by animateColorAsState(if (focused) Color(0xFF9CD7FF) else Color.White.copy(alpha = 0.12f))
    val scale by animateFloatAsState(if (focused) 1.025f else 1f)
    val action = {
        if (access) {
            MusicReader.togglePlayback(context)
        } else {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x71111A29))
            .border(if (focused) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = action, onLongClick = action)
            .focusable()
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF25344A)),
            contentAlignment = Alignment.Center,
        ) {
            if (nowPlaying?.albumArt != null) {
                Image(
                    bitmap = nowPlaying!!.albumArt!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LumaIcon(Icons.Rounded.MusicNote, "Music", Color(0xFFC7A7FF), 34.dp)
            }
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = when {
                    !access -> "Enable music widget"
                    nowPlaying == null -> "Nothing playing"
                    else -> nowPlaying!!.title
                },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    !access -> "Press OK to allow access"
                    nowPlaying == null -> "Start music in any app"
                    else -> nowPlaying!!.artist
                },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = nowPlaying?.appName ?: "Music",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        LumaIcon(
            image = if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            description = "Play or pause",
            tint = Color.White,
            size = 29.dp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetShell(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusScale = if (focused) 1.015f else 1f
    val interactionModifier = if (onClick != null) {
        Modifier
            .scale(focusScale)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .focusable()
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .then(interactionModifier)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x71111A29))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color(0xFF9CD7FF) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(22.dp),
            )
            .padding(17.dp),
        content = content,
    )
}

private data class ForecastLoad(
    val forecast: WeatherForecast? = null,
    val failed: Boolean = false,
)

@Composable
private fun ForecastScreen(
    preferences: LauncherPreferences,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val state by produceState(
        initialValue = ForecastLoad(),
        preferences.weatherLocation.key,
        preferences.useFahrenheit,
    ) {
        value = WeatherRepository.forecast(
            preferences.weatherLocation,
            preferences.useFahrenheit,
        ).fold(
            onSuccess = { ForecastLoad(forecast = it) },
            onFailure = { ForecastLoad(failed = true) },
        )
    }
    val unit = if (preferences.useFahrenheit) "°F" else "°C"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20A0E16))
            .padding(horizontal = 68.dp, vertical = 38.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = state.forecast?.let { "${it.locationName} forecast" } ?: "7-day forecast",
                    color = Color.White,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Weather updates automatically from your selected location.",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            FocusButton("Back", Icons.Rounded.ArrowBack, onClose, requestInitialFocus = true)
        }

        when {
            state.failed -> Text(
                "The forecast could not load. Check the internet connection and try again.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 18.sp,
            )
            state.forecast == null -> Text(
                "Loading forecast…",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 18.sp,
            )
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(state.forecast!!.days.size) { index ->
                    ForecastDayCard(
                        day = state.forecast!!.days[index],
                        unit = unit,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastDayCard(day: ForecastDay, unit: String) {
    var focused by remember { mutableStateOf(false) }
    val dayName = remember(day.date) {
        runCatching {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day.date)
            SimpleDateFormat("EEE", Locale.getDefault()).format(date!!)
        }.getOrDefault(day.date)
    }
    Column(
        modifier = Modifier
            .width(145.dp)
            .height(235.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (focused) Color(0xFF243B52) else Color(0xB3151F2D))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color(0xFF9CD7FF) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(22.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(dayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(day.symbol, color = Color(0xFFFFD784), fontSize = 43.sp)
        Text(
            day.description,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${day.high}$unit  /  ${day.low}$unit",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "Rain ${day.precipitationChance}%",
            color = Color(0xFF9CD7FF),
            fontSize = 12.sp,
        )
    }
}

private fun infoColor(connected: Boolean) = if (connected) Color(0xFF79E6B2) else Color(0xFFFF9C9C)

@Composable
private fun SectionTitle(icon: ImageVector, title: String, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LumaIcon(icon, title, Color(0xFF9CD7FF), 21.dp)
        Spacer(Modifier.width(9.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        Text(hint, color = Color.White.copy(alpha = 0.42f), fontSize = 13.sp)
    }
}

@Composable
private fun EmptyFavorites() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LumaIcon(Icons.Rounded.Star, "Favorites", Color(0xFFFFD784), 31.dp)
        Spacer(Modifier.width(15.dp))
        Column {
            Text("Your favorite bar is ready", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                "Move to any app below, then hold the OK button to add it here.",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 13.sp,
            )
        }
    }
}

private data class CardDimensions(val width: Dp, val height: Dp, val icon: Dp)

private fun cardDimensions(size: CardSize) = when (size) {
    CardSize.COMPACT -> CardDimensions(148.dp, 104.dp, 58.dp)
    CardSize.COZY -> CardDimensions(180.dp, 126.dp, 72.dp)
    CardSize.LARGE -> CardDimensions(216.dp, 150.dp, 88.dp)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(
    app: TvApp,
    preferences: LauncherPreferences,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    favorite: Boolean = true,
    requestInitialFocus: Boolean = false,
    interactive: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus, interactive) {
        if (requestInitialFocus && interactive) {
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }
    val scale = if (focused) 1.02f else 1f
    val dimensions = cardDimensions(preferences.cardSize)
    val cardShape: Shape = when (preferences.cardShape) {
        CardShapeStyle.ROUNDED -> RoundedCornerShape(20.dp)
        CardShapeStyle.SOFT -> RoundedCornerShape(38.dp)
        CardShapeStyle.SQUARE -> RoundedCornerShape(5.dp)
        CardShapeStyle.CUT -> CutCornerShape(18.dp)
    }
    val selectedColor = when (preferences.cardColor) {
        CardColor.NEUTRAL -> Color(0xFF526176)
        CardColor.OCEAN -> Color(0xFF176EA3)
        CardColor.VIOLET -> Color(0xFF6746A5)
        CardColor.TEAL -> Color(0xFF177B74)
        CardColor.EMBER -> Color(0xFFA34A38)
    }
    val shadeAmount = when (preferences.cardShade) {
        CardShade.LIGHT -> 0.18f
        CardShade.MEDIUM -> 0.43f
        CardShade.DARK -> 0.68f
    }
    val shadedColor = lerp(selectedColor, Color.Black, shadeAmount)
    val background = when (preferences.cardStyle) {
        CardStyle.GLASS -> shadedColor.copy(alpha = 0.66f)
        CardStyle.SOLID -> shadedColor
        CardStyle.OUTLINE -> shadedColor.copy(alpha = 0.22f)
        CardStyle.GLOW -> shadedColor.copy(alpha = 0.82f)
    }
    val restingBorder = when (preferences.cardStyle) {
        CardStyle.OUTLINE -> selectedColor.copy(alpha = 0.78f)
        CardStyle.GLOW -> selectedColor.copy(alpha = 0.88f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    val borderColor = if (focused) Color(0xFFE1F6FF) else restingBorder

    var cardModifier = Modifier
        .width(dimensions.width)
        .height(dimensions.height)
        .scale(scale)
        .shadow(
            if (preferences.cardStyle == CardStyle.GLOW && focused) 6.dp else 0.dp,
            cardShape,
        )
        .clip(cardShape)
        .background(background)
        .border(if (focused) 3.dp else 1.dp, borderColor, cardShape)
        .padding(horizontal = 12.dp, vertical = 10.dp)
    if (interactive) {
        cardModifier = cardModifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .focusable()
    }

    Column(
        modifier = cardModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            StyledAppIcon(
                bitmap = app.icon,
                style = if (app.isFromIconPack) IconStyle.CLEAN else preferences.iconStyle,
                size = if (preferences.showCardNames) dimensions.icon else dimensions.icon * 1.12f,
                seed = app.packageName,
            )
            if (favorite) {
                Box(
                    Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD784)),
                    contentAlignment = Alignment.Center,
                ) {
                    LumaIcon(Icons.Rounded.Star, "Favorite", Color(0xFF3B2C05), 11.dp)
                }
            }
        }
        if (preferences.showCardNames) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = if (preferences.cardSize == CardSize.COMPACT) 12.sp else 14.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppCardSettingsPreview(
    app: TvApp,
    preferences: LauncherPreferences,
) {
    Column(
        modifier = Modifier
            .width(330.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.24f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("Live card preview", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AppCard(
                app = app,
                preferences = preferences,
                favorite = false,
                onClick = {},
                onLongClick = {},
                interactive = false,
            )
        }
        Text(
            "${preferences.cardStyle.title} | ${preferences.cardShape.title} | ${preferences.cardColor.title} | ${preferences.cardShade.title}",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 13.sp,
        )
        Text(
            "Every choice updates this card immediately.",
            color = Color.White.copy(alpha = 0.52f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun StyledAppIcon(bitmap: ImageBitmap, style: IconStyle, size: Dp, seed: String) {
    val palettes = remember {
        listOf(
            listOf(Color(0xFF87E4FF), Color(0xFF7776E9)),
            listOf(Color(0xFFFFB5D8), Color(0xFFE05AA8)),
            listOf(Color(0xFFFFDA77), Color(0xFFFF8B62)),
            listOf(Color(0xFF91F1C2), Color(0xFF27A68B)),
            listOf(Color(0xFFD6B2FF), Color(0xFF8266D4)),
        )
    }
    val palette = palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size]

    when (style) {
        IconStyle.CLEAN -> Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(size * 0.96f),
            contentScale = ContentScale.Fit,
        )

        IconStyle.RAISED -> {
            val shape = RoundedCornerShape(24)
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(size * 0.84f)
                        .offset(x = 4.dp, y = 5.dp)
                        .background(Color.Black.copy(alpha = 0.48f), shape),
                )
                Box(
                    Modifier
                        .size(size * 0.84f)
                        .shadow(9.dp, shape)
                        .background(lerp(palette.first(), Color(0xFF182131), 0.58f), shape)
                        .border(1.dp, Color.White.copy(alpha = 0.30f), shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(bitmap, null, Modifier.size(size * 0.70f), contentScale = ContentScale.Fit)
                }
            }
        }

        IconStyle.STICKER -> {
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                listOf(-3.dp to 0.dp, 3.dp to 0.dp, 0.dp to -3.dp, 0.dp to 3.dp).forEach { (x, y) ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.82f).offset(x = x, y = y).rotate(-4f),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
                Image(bitmap, null, Modifier.size(size * 0.78f).rotate(-4f), contentScale = ContentScale.Fit)
            }
        }

        IconStyle.NEON -> {
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(size * 0.88f)
                        .shadow(14.dp, CircleShape)
                        .background(Color(0xFF080B12), CircleShape)
                        .border(3.dp, Brush.linearGradient(palette), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(bitmap, null, Modifier.size(size * 0.67f), contentScale = ContentScale.Fit)
                }
            }
        }

        IconStyle.BUBBLE -> {
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(size * 0.90f)
                        .shadow(8.dp, CircleShape)
                        .background(Brush.radialGradient(listOf(palette.first(), palette.last())), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.44f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(bitmap, null, Modifier.size(size * 0.68f), contentScale = ContentScale.Fit)
                }
                Box(
                    Modifier
                        .size(size * 0.15f)
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = 6.dp)
                        .background(Color.White.copy(alpha = 0.52f), CircleShape),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusButton(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    requestInitialFocus: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            delay(80)
            runCatching { focusRequester.requestFocus() }
        }
    }
    val background by animateColorAsState(if (focused) Color(0xFFEDF8FF) else Color(0x33182434))
    val content by animateColorAsState(if (focused) Color(0xFF0B1726) else Color.White)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            LumaIcon(icon, label, content, 19.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsScreen(
    apps: List<TvApp>,
    preferences: LauncherPreferences,
    updateInfo: UpdateInfo?,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val installedIconPacks by produceState<List<IconPackInfo>>(emptyList()) {
        value = IconPackRepository.discover(context)
    }
    val homeHelperEnabled by produceState(false) {
        while (true) {
            value = LumaHomeAccessibilityService.isEnabled(context)
            delay(1_000)
        }
    }
    var pendingIconPackage by remember { mutableStateOf<String?>(null) }
    var showHomeHelperDisclosure by remember { mutableStateOf(false) }
    BackHandler {
        if (showHomeHelperDisclosure) showHomeHelperDisclosure = false else onClose()
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        preferences.setCustomBackground(uri)
    }
    val customIconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val packageName = pendingIconPackage
        pendingIconPackage = null
        if (uri == null || packageName == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        preferences.setCustomIcon(packageName, uri)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF20A0E16))
            .padding(horizontal = 68.dp, vertical = 38.dp),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Make Luma yours", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Changes save automatically.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                FocusButton("Done", Icons.Rounded.Close, onClose, requestInitialFocus = true)
            }

            SettingSection(
                "Android TV home screen",
                "Choose Luma as Home. The optional helper keeps Luma open on Google TV devices that ignore that choice.",
            ) {
                ChoiceRow {
                    SettingChoice("Choose Luma as default Home", false, icon = Icons.Rounded.Home) {
                        openDefaultHomeChooser(context)
                    }
                    SettingChoice("Open Android settings", false, icon = Icons.Rounded.Settings) {
                        openAndroidSettings(context)
                    }
                    SettingChoice(
                        label = if (homeHelperEnabled) "Home helper is on" else "Enable Home helper",
                        selected = homeHelperEnabled,
                        icon = Icons.Rounded.Home,
                    ) {
                        if (homeHelperEnabled) {
                            openAccessibilitySettings(context)
                        } else {
                            showHomeHelperDisclosure = true
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (homeHelperEnabled) {
                        "Luma Home screen helper is enabled. The Home button should return to Luma."
                    } else {
                        "In Accessibility, select ‘Luma Home screen helper’ and turn it on once."
                    },
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                )
            }

            SettingSection(
                "App updates",
                if (BuildConfig.IS_PLAY_STORE_BUILD) {
                    "Google Play keeps this version current automatically."
                } else {
                    "Luma checks GitHub automatically. Android will still ask you to approve each installation."
                },
            ) {
                Text(
                    if (BuildConfig.IS_PLAY_STORE_BUILD) {
                        "Installed version ${BuildConfig.VERSION_NAME} - managed by Google Play"
                    } else if (updateInfo == null) {
                        "Installed version ${BuildConfig.VERSION_NAME}"
                    } else {
                        "Version ${updateInfo.versionName} is ready (installed: ${BuildConfig.VERSION_NAME})."
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                ChoiceRow {
                    SettingChoice(
                        label = if (BuildConfig.IS_PLAY_STORE_BUILD) {
                            "Open Google Play"
                        } else if (updateInfo == null) {
                            "Download newest version"
                        } else {
                            "Install ${updateInfo.versionName}"
                        },
                        selected = !BuildConfig.IS_PLAY_STORE_BUILD && updateInfo != null,
                        icon = Icons.Rounded.SystemUpdateAlt,
                    ) {
                        if (BuildConfig.IS_PLAY_STORE_BUILD) {
                            openPlayStorePage(context)
                        } else {
                            openWebUrl(
                                context,
                                updateInfo?.downloadUrl ?: UpdateRepository.STABLE_DOWNLOAD_URL,
                            )
                        }
                    }
                }
            }

            SettingSection("Background", "Choose a built-in scene or use one of your photos.") {
                ChoiceRow {
                    BackgroundStyle.entries.filter { it != BackgroundStyle.CUSTOM }.forEach { style ->
                        SettingChoice(style.title, preferences.backgroundStyle == style) {
                            preferences.setBackground(style)
                        }
                    }
                    SettingChoice(
                        label = if (preferences.customBackgroundUri == null) "Choose photo" else "My photo",
                        selected = preferences.backgroundStyle == BackgroundStyle.CUSTOM,
                        icon = Icons.Rounded.ImageIcon,
                    ) {
                        imagePicker.launch(arrayOf("image/*"))
                    }
                }
            }

            SettingSection("App cards", "Preview the result here, then mix a surface, shape, shade, color, and size.") {
                val previewApp = apps.firstOrNull { app ->
                    app.packageName == "com.hulu.livingroomplus" || app.packageName == "com.google.android.youtube.tvunplugged"
                } ?: apps.firstOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SettingLabel("Card names")
                        ChoiceRow {
                            SettingChoice(
                                label = if (preferences.showCardNames) "Names on" else "Names off",
                                selected = preferences.showCardNames,
                            ) {
                                preferences.updateShowCardNames(!preferences.showCardNames)
                            }
                        }
                        SettingLabel("Surface")
                        ChoiceRow {
                            CardStyle.entries.forEach { style ->
                                SettingChoice(style.title, preferences.cardStyle == style) { preferences.updateCardStyle(style) }
                            }
                        }
                        SettingLabel("Shape")
                        ChoiceRow {
                            CardShapeStyle.entries.forEach { shape ->
                                SettingChoice(shape.title, preferences.cardShape == shape) { preferences.updateCardShape(shape) }
                            }
                        }
                        SettingLabel("Background shade")
                        ChoiceRow {
                            CardShade.entries.forEach { shade ->
                                SettingChoice(shade.title, preferences.cardShade == shade) { preferences.updateCardShade(shade) }
                            }
                        }
                        SettingLabel("Color")
                        ChoiceRow {
                            CardColor.entries.forEach { color ->
                                SettingChoice(color.title, preferences.cardColor == color) { preferences.updateCardColor(color) }
                            }
                        }
                        SettingLabel("Size")
                        ChoiceRow {
                            CardSize.entries.forEach { size ->
                                SettingChoice(size.title, preferences.cardSize == size) { preferences.updateCardSize(size) }
                            }
                        }
                    }
                    if (previewApp != null) {
                        AppCardSettingsPreview(previewApp, preferences)
                    }
                }
            }

            SettingSection("Real icon packs", "Use a complete installed icon pack with artwork made for each app.") {
                ChoiceRow {
                    SettingChoice("Built-in styles", preferences.iconPackPackage == null) {
                        preferences.updateIconPack(null)
                    }
                    installedIconPacks.forEach { pack ->
                        SettingChoice(pack.title, preferences.iconPackPackage == pack.packageName) {
                            preferences.updateIconPack(pack.packageName)
                        }
                    }
                }
                if (installedIconPacks.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No compatible icon pack is installed yet. Good choices: Morphic 3D, Auric Dark, Comics, or Crayon.",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp,
                    )
                }
            }

            SettingSection("Built-in icon look", "Used when no icon pack is selected, and for apps a pack does not cover.") {
                val previewApp = apps.firstOrNull()
                ChoiceRow {
                    IconStyle.entries.forEach { style ->
                        SettingChoice(
                            label = style.title,
                            selected = preferences.iconPackPackage == null && preferences.iconStyle == style,
                            preview = previewApp?.let { app ->
                                {
                                    StyledAppIcon(
                                        bitmap = app.icon,
                                        style = style,
                                        size = 38.dp,
                                        seed = app.packageName,
                                    )
                                }
                            },
                        ) {
                            preferences.updateIconPack(null)
                            preferences.updateIconStyle(style)
                        }
                    }
                }
            }

            SettingSection("Weather", "Use the TV's approximate internet location or choose a city.") {
                ChoiceRow {
                    SettingChoice(
                        WeatherLocation.automatic.name,
                        preferences.weatherLocation.automatic,
                    ) {
                        preferences.updateWeatherLocation(WeatherLocation.automatic)
                    }
                    WeatherLocation.presets.forEach { location ->
                        SettingChoice(location.name, preferences.weatherLocation.key == location.key) {
                            preferences.updateWeatherLocation(location)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ChoiceRow {
                    SettingChoice("Fahrenheit", preferences.useFahrenheit) { preferences.setFahrenheit(true) }
                    SettingChoice("Celsius", !preferences.useFahrenheit) { preferences.setFahrenheit(false) }
                }
            }

            SettingSection("Favorite bar", "Reorder favorites, replace their actual icon image, or remove them.") {
                val favoriteApps = preferences.favoritePackages.mapNotNull { packageName ->
                    apps.firstOrNull { it.packageName == packageName }
                }
                if (favoriteApps.isEmpty()) {
                    Text(
                        "No favorites yet. Close settings and hold OK on any app card.",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 14.sp,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        favoriteApps.forEachIndexed { index, app ->
                            FavoriteEditorRow(
                                app = app,
                                canMoveLeft = index > 0,
                                canMoveRight = index < favoriteApps.lastIndex,
                                onMoveLeft = { preferences.moveFavorite(app.packageName, -1) },
                                onMoveRight = { preferences.moveFavorite(app.packageName, 1) },
                                onChangeIcon = {
                                    pendingIconPackage = app.packageName
                                    customIconPicker.launch(arrayOf("image/*"))
                                },
                                onResetIcon = { preferences.removeCustomIcon(app.packageName) },
                                hasCustomIcon = preferences.customIconUris.containsKey(app.packageName),
                                onRemove = { preferences.toggleFavorite(app.packageName) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(34.dp))
        }

        if (showHomeHelperDisclosure) {
            HomeHelperDisclosure(
                onCancel = { showHomeHelperDisclosure = false },
                onContinue = {
                    showHomeHelperDisclosure = false
                    openAccessibilitySettings(context)
                },
            )
        }
    }
}

@Composable
private fun HomeHelperDisclosure(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE600050A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(720.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF142234))
                .border(2.dp, Color(0xFF72D5FF), RoundedCornerShape(26.dp))
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Before enabling the Home helper",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Luma uses Android Accessibility only to detect when the stock Google TV Home screen appears, then reopen Luma. It receives the name of that Home app when the screen changes.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 16.sp,
            )
            Text(
                "Luma cannot read screen content, typed text, passwords, or personal information. It does not store, send, sell, or share Accessibility data.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
            )
            Text(
                "The helper is optional. If you continue, Android will open Accessibility so you can choose whether to enable it.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FocusButton("Not now", Icons.Rounded.Close, onCancel, requestInitialFocus = true)
                Spacer(Modifier.width(12.dp))
                FocusButton("I understand - continue", Icons.Rounded.Check, onContinue)
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
            .padding(20.dp),
    ) {
        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun SettingLabel(label: String) {
    Text(
        text = label,
        color = Color.White.copy(alpha = 0.68f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun ChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingChoice(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    preview: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color(0xFFEAF8FF)
        selected -> Color(0xFF244B63)
        else -> Color(0xFF151E2C)
    }
    val fg = if (focused) Color(0xFF07121F) else Color.White
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(
                width = if (selected || focused) 2.dp else 1.dp,
                color = if (selected) Color(0xFF72D5FF) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(13.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .focusable()
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (preview != null) {
            preview()
            Spacer(Modifier.width(9.dp))
        }
        if (icon != null) {
            LumaIcon(icon, label, fg, 17.dp)
            Spacer(Modifier.width(7.dp))
        }
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (selected) {
            Spacer(Modifier.width(7.dp))
            LumaIcon(Icons.Rounded.Check, "Selected", fg, 16.dp)
        }
    }
}

@Composable
private fun FavoriteEditorRow(
    app: TvApp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onChangeIcon: () -> Unit,
    onResetIcon: () -> Unit,
    hasCustomIcon: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x66141D2B))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(app.icon, app.label, Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)))
        Spacer(Modifier.width(12.dp))
        Text(app.label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (canMoveLeft) FocusButton("Left", Icons.Rounded.ArrowBack, onMoveLeft)
        Spacer(Modifier.width(7.dp))
        if (canMoveRight) FocusButton("Right", Icons.Rounded.ArrowForward, onMoveRight)
        Spacer(Modifier.width(7.dp))
        FocusButton("Change icon", Icons.Rounded.ImageIcon, onChangeIcon)
        if (hasCustomIcon) {
            Spacer(Modifier.width(7.dp))
            FocusButton("Reset icon", Icons.Rounded.Close, onResetIcon)
        }
        Spacer(Modifier.width(7.dp))
        FocusButton("Remove", Icons.Rounded.Close, onRemove)
    }
}

@Composable
private fun LumaIcon(
    image: ImageVector,
    description: String,
    tint: Color,
    size: Dp,
) {
    Image(
        imageVector = image,
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(size),
    )
}
