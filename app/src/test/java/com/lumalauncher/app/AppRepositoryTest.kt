package com.lumalauncher.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRepositoryTest {
    @Test
    fun recognizesCommonTvMusicApps() {
        assertTrue(AppRepository.isLikelyMusicApp("com.spotify.tv.android", "Spotify"))
        assertTrue(AppRepository.isLikelyMusicApp("com.pandora.android.atv", "Pandora"))
        assertTrue(AppRepository.isLikelyMusicApp("com.example.player", "Amazon Music"))
        assertFalse(AppRepository.isLikelyMusicApp("com.hulu.livingroomplus", "Hulu"))
    }
}
