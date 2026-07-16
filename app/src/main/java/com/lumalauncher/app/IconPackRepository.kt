package com.lumalauncher.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Locale

object IconPackRepository {
    private val themeIntents = listOf(
        Intent("org.adw.launcher.THEMES"),
        Intent("com.gau.go.launcherex.theme"),
        Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME"),
    )

    suspend fun discover(context: Context): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        themeIntents
            .flatMap { queryActivities(packageManager, it) }
            .distinctBy { it.activityInfo.packageName }
            .map { resolved ->
                IconPackInfo(
                    packageName = resolved.activityInfo.packageName,
                    title = resolved.loadLabel(packageManager).toString()
                        .ifBlank { resolved.activityInfo.packageName },
                )
            }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    suspend fun apply(
        context: Context,
        apps: List<TvApp>,
        iconPackPackage: String,
    ): List<TvApp> = withContext(Dispatchers.IO) {
        runCatching {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackage)
            val mappings = readMappings(resources, iconPackPackage)
            apps.map { app ->
                val fullComponent = "${app.packageName}/${app.activityName}"
                val shortActivity = app.activityName.removePrefix(app.packageName)
                val shortComponent = "${app.packageName}/$shortActivity"
                val drawableName = mappings[fullComponent]
                    ?: mappings[shortComponent]
                    ?: mappings[app.packageName]
                    ?: return@map app
                val drawableId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (drawableId == 0) return@map app
                val drawable = resources.getDrawable(drawableId, null) ?: return@map app
                app.copy(
                    icon = AppRepository.drawableToImageBitmap(drawable),
                    isFromIconPack = true,
                )
            }
        }.getOrDefault(apps)
    }

    private fun readMappings(resources: Resources, packageName: String): Map<String, String> {
        val resourceId = resources.getIdentifier("appfilter", "xml", packageName)
        return if (resourceId != 0) {
            parse(resources.getXml(resourceId))
        } else {
            runCatching {
                resources.assets.open("appfilter.xml").use { parse(it) }
            }.getOrDefault(emptyMap())
        }
    }

    private fun parse(inputStream: InputStream): Map<String, String> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(inputStream, "UTF-8")
        }
        return parse(parser)
    }

    private fun parse(parser: XmlPullParser): Map<String, String> {
        val mappings = linkedMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                val component = parser.getAttributeValue(null, "component")
                    ?.removePrefix("ComponentInfo{")
                    ?.removeSuffix("}")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    mappings[component] = drawable
                    component.substringBefore('/').takeIf(String::isNotBlank)?.let { packageOnly ->
                    if (!mappings.containsKey(packageOnly)) {
                        mappings[packageOnly] = drawable
                    }
                    }
                }
            }
            event = parser.next()
        }
        return mappings
    }

    @Suppress("DEPRECATION")
    private fun queryActivities(packageManager: PackageManager, intent: Intent) =
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }
}
