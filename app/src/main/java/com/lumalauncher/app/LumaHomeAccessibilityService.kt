package com.lumalauncher.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class LumaHomeAccessibilityService : AccessibilityService() {
    private var lastHomeRedirectAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in STOCK_TV_LAUNCHERS) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastHomeRedirectAt < 1_500L) return
        lastHomeRedirectAt = now

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        private val STOCK_TV_LAUNCHERS = setOf(
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcher",
            "com.android.tv.launcher",
        )

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
