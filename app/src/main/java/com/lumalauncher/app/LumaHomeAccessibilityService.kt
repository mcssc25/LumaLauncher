package com.lumalauncher.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class LumaHomeAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastHomeRedirectAt = 0L
    private var lastStockHomeSeenAt = 0L
    private var systemHomeActivities: Set<String> = emptySet()
    private val firstHomeRetry = Runnable { retryRecentHomeRedirect("first_retry") }
    private val finalHomeRetry = Runnable { retryRecentHomeRedirect("final_retry") }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        systemHomeActivities = findSystemHomeActivities()
        Log.i(TAG, "Connected; built-in Home activities: ${systemHomeActivities.sorted().joinToString()}")

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 0
            packageNames = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in WATCHED_WINDOW_EVENTS) return
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString()
        Log.d(TAG, "Window event ${event.eventType} from $packageName/$className")
        if (!HomeOverridePolicy.shouldRedirectFromWindow(
                packageName = packageName,
                className = className,
                systemHomeActivities = systemHomeActivities,
                fallbackHomePackages = KNOWN_STOCK_TV_LAUNCHERS,
            )
        ) return

        redirectToLuma("stock_home_window")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.d(TAG, "Home key received")
            redirectToLuma("home_key")
        }

        // Consume both halves of the Home key press so Google Home does not open behind Luma.
        return true
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(firstHomeRetry)
        mainHandler.removeCallbacks(finalHomeRetry)
        isServiceConnected = false
        super.onDestroy()
    }

    private fun redirectToLuma(source: String) {
        val now = SystemClock.elapsedRealtime()
        lastStockHomeSeenAt = now
        if (now - lastHomeRedirectAt >= HOME_REDIRECT_DEBOUNCE_MS) {
            lastHomeRedirectAt = now
            launchLuma(source)
        }

        mainHandler.removeCallbacks(firstHomeRetry)
        mainHandler.removeCallbacks(finalHomeRetry)
        mainHandler.postDelayed(firstHomeRetry, FIRST_HOME_RETRY_MS)
        mainHandler.postDelayed(finalHomeRetry, FINAL_HOME_RETRY_MS)
    }

    private fun retryRecentHomeRedirect(retryName: String) {
        if (SystemClock.elapsedRealtime() - lastStockHomeSeenAt <= HOME_RETRY_WINDOW_MS) {
            launchLuma(retryName)
        }
    }

    private fun launchLuma(source: String) {
        Log.i(TAG, "Returning to Luma from $source")

        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    )
                },
            )
        }
    }

    private fun findSystemHomeActivities(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            packageManager.queryIntentActivities(homeIntent, 0)
                .mapNotNull { resolved ->
                    val applicationInfo = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null
                    val isSystemApp = applicationInfo.flags and
                        (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    resolved.activityInfo?.let { activityInfo ->
                        HomeOverridePolicy.activityId(activityInfo.packageName, activityInfo.name).takeIf {
                            isSystemApp && activityInfo.packageName != this.packageName
                        }
                    }
                }
                .toSet()
        }.getOrDefault(emptySet())
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "LumaHomeHelper"
        private const val HOME_REDIRECT_DEBOUNCE_MS = 250L
        private const val FIRST_HOME_RETRY_MS = 220L
        private const val FINAL_HOME_RETRY_MS = 700L
        private const val HOME_RETRY_WINDOW_MS = 1_200L
        private val WATCHED_WINDOW_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
        )
        private val KNOWN_STOCK_TV_LAUNCHERS = setOf(
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcher",
            "com.android.tv.launcher",
        )

        @Volatile
        private var isServiceConnected = false

        fun status(context: Context): HomeHelperStatus = when {
            !isEnabled(context) -> HomeHelperStatus.OFF
            isServiceConnected -> HomeHelperStatus.CONNECTED
            else -> HomeHelperStatus.ENABLED_NOT_CONNECTED
        }

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, LumaHomeAccessibilityService::class.java)
                .flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            return enabled.split(':').any { flattened ->
                ComponentName.unflattenFromString(flattened) ==
                    ComponentName.unflattenFromString(expected)
            }
        }
    }
}

enum class HomeHelperStatus {
    OFF,
    ENABLED_NOT_CONNECTED,
    CONNECTED,
}

internal object HomeOverridePolicy {
    fun activityId(packageName: String, className: String): String {
        val fullClassName = if (className.startsWith('.')) "$packageName$className" else className
        return "$packageName/$fullClassName"
    }

    fun shouldRedirectFromWindow(
        packageName: String,
        className: String?,
        systemHomeActivities: Set<String>,
        fallbackHomePackages: Set<String>,
    ): Boolean {
        if (className.isNullOrBlank()) return packageName in fallbackHomePackages
        return activityId(packageName, className) in systemHomeActivities
    }
}
