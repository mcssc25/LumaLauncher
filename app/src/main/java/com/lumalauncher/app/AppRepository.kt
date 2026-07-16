package com.lumalauncher.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object AppRepository {
    suspend fun loadLaunchableApps(context: Context): List<TvApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val discovered = linkedMapOf<String, TvApp>()

        listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            queryActivities(packageManager, intent).forEach { resolved ->
                val activity = resolved.activityInfo ?: return@forEach
                val packageName = activity.packageName
                if (packageName == context.packageName || discovered.containsKey(packageName)) return@forEach

                val icon = runCatching { resolved.loadIcon(packageManager) }
                    .getOrElse { context.applicationInfo.loadIcon(packageManager) }
                discovered[packageName] = TvApp(
                    label = resolved.loadLabel(packageManager).toString().ifBlank { packageName },
                    packageName = packageName,
                    activityName = activity.name,
                    icon = drawableToImageBitmap(icon),
                )
            }
        }

        discovered.values.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun launch(context: Context, app: TvApp) {
        val explicit = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(explicit) }
            .recoverCatching {
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let(context::startActivity)
            }
    }

    @Suppress("DEPRECATION")
    private fun queryActivities(packageManager: PackageManager, intent: Intent) =
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }

    internal fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        return drawable.toBitmap().asImageBitmap()
    }

    private fun Drawable.toBitmap(): Bitmap {
        val size = 192
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }
}
