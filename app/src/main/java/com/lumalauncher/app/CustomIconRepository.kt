package com.lumalauncher.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object CustomIconRepository {
    fun apply(
        context: Context,
        apps: List<TvApp>,
        customIconUris: Map<String, String>,
    ): List<TvApp> = apps.map { app ->
        val custom = customIconUris[app.packageName]?.let { loadUri(context, Uri.parse(it)) }
        val replacement = custom ?: bundledReplacement(context, app)
        if (replacement == null) app else app.copy(icon = replacement, isFromIconPack = true)
    }

    private fun bundledReplacement(context: Context, app: TvApp): ImageBitmap? {
        val resource = bundledResourceFor(app.label, app.packageName) ?: return null
        return context.getDrawable(resource)?.let(AppRepository::drawableToImageBitmap)
    }

    internal fun bundledResourceFor(appLabel: String, appPackageName: String): Int? {
        val label = appLabel.lowercase()
        val packageName = appPackageName.lowercase()
        return when {
            "nuvio" in label || "nuvio" in packageName -> R.drawable.custom_icon_nuvio
            "tivimate" in label || "tivi mate" in label || "tivimate" in packageName ||
                ("8k" in label && ("tv" in label || "tivi" in label)) -> R.drawable.custom_icon_8k_tivimate
            label == "mlb" || label.startsWith("mlb ") || ".mlb" in packageName -> R.drawable.custom_icon_mlb
            label == "netflix" || packageName == "com.netflix.ninja" ||
                packageName == "com.netflix.mediaclient" -> R.drawable.custom_icon_netflix
            "prime video" in label || packageName == "com.amazon.amazonvideo.livingroom" ||
                packageName == "com.amazon.avod.thirdpartyclient" -> R.drawable.custom_icon_prime_video
            label == "youtube" || packageName == "com.google.android.youtube.tv" ||
                packageName == "com.google.android.youtube" -> R.drawable.custom_icon_youtube
            "downloader" in label || packageName == "com.esaba.downloader" -> R.drawable.custom_icon_downloader
            label == "play store" || label == "google play store" ||
                packageName == "com.android.vending" -> R.drawable.custom_icon_play_store
            label == "hulu" || packageName == "com.hulu.livingroomplus" ||
                packageName == "com.hulu.plus" -> R.drawable.custom_icon_hulu
            "speed test" in label || "speedtest" in label || packageName == "com.rma.speedtesttv" ||
                "ookla" in packageName -> R.drawable.custom_icon_speed_test
            label == "play games" || label == "google play games" ||
                packageName == "com.google.android.play.games" -> R.drawable.custom_icon_play_games
            label == "play movies & tv" || label == "google play movies & tv" ||
                label == "google tv" || packageName == "com.google.android.videos" ->
                R.drawable.custom_icon_play_movies
            "surfshark" in label || packageName == "com.surfshark.vpnclient.android" ->
                R.drawable.custom_icon_surfshark
            "stremio" in label || packageName == "com.stremio.one" ||
                packageName == "com.stremio.two" -> R.drawable.custom_icon_stremio
            label == "youtube tv" || packageName == "com.google.android.youtube.tvunplugged" ->
                R.drawable.custom_icon_youtube_tv
            label == "spotify" || packageName == "com.spotify.tv.android" ||
                packageName == "com.spotify.music" -> R.drawable.custom_icon_spotify
            else -> null
        }
    }

    private fun loadUri(context: Context, uri: Uri): ImageBitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
        }
    }.getOrNull()
}
