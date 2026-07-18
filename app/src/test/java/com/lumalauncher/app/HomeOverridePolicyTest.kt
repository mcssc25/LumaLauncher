package com.lumalauncher.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOverridePolicyTest {
    private val systemHomeActivities = setOf(
        "com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.HomeActivity",
        "com.vendor.stocklauncher/com.vendor.stocklauncher.TvHomeActivity",
    )
    private val fallbackHomePackages = setOf("com.google.android.apps.tv.launcherx")

    @Test
    fun redirectsWhenAStockHomeActivityAppears() {
        assertTrue(
            HomeOverridePolicy.shouldRedirectFromWindow(
                packageName = "com.google.android.apps.tv.launcherx",
                className = "com.google.android.apps.tv.launcherx.HomeActivity",
                systemHomeActivities = systemHomeActivities,
                fallbackHomePackages = fallbackHomePackages,
            ),
        )
        assertTrue(
            HomeOverridePolicy.shouldRedirectFromWindow(
                packageName = "com.vendor.stocklauncher",
                className = ".TvHomeActivity",
                systemHomeActivities = systemHomeActivities,
                fallbackHomePackages = fallbackHomePackages,
            ),
        )
    }

    @Test
    fun doesNotRedirectOtherActivitiesInsideTheStockLauncher() {
        assertFalse(
            HomeOverridePolicy.shouldRedirectFromWindow(
                packageName = "com.google.android.apps.tv.launcherx",
                className = "com.google.android.apps.tv.launcherx.ProfilePickerActivity",
                systemHomeActivities = systemHomeActivities,
                fallbackHomePackages = fallbackHomePackages,
            ),
        )
    }

    @Test
    fun fallsBackToKnownStockPackageWhenAndroidOmitsTheClassName() {
        assertTrue(
            HomeOverridePolicy.shouldRedirectFromWindow(
                packageName = "com.google.android.apps.tv.launcherx",
                className = null,
                systemHomeActivities = systemHomeActivities,
                fallbackHomePackages = fallbackHomePackages,
            ),
        )
        assertFalse(
            HomeOverridePolicy.shouldRedirectFromWindow(
                packageName = "com.spotify.tv.android",
                className = null,
                systemHomeActivities = systemHomeActivities,
                fallbackHomePackages = fallbackHomePackages,
            ),
        )
    }
}
