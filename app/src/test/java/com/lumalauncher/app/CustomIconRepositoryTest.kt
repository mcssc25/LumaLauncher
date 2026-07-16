package com.lumalauncher.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomIconRepositoryTest {
    @Test
    fun mapsNamedAppsToFullReplacementArtwork() {
        assertEquals(
            R.drawable.custom_icon_nuvio,
            CustomIconRepository.bundledResourceFor("Nuvio", "app.nuvio.tv"),
        )
        assertEquals(
            R.drawable.custom_icon_8k_tivimate,
            CustomIconRepository.bundledResourceFor("8K TiviMate", "ar.tvplayer.tv"),
        )
        assertEquals(
            R.drawable.custom_icon_mlb,
            CustomIconRepository.bundledResourceFor("MLB", "com.example.baseball"),
        )
        assertEquals(
            R.drawable.custom_icon_netflix,
            CustomIconRepository.bundledResourceFor("Netflix", "com.netflix.ninja"),
        )
        assertEquals(
            R.drawable.custom_icon_prime_video,
            CustomIconRepository.bundledResourceFor("Prime Video", "com.amazon.amazonvideo.livingroom"),
        )
        assertEquals(
            R.drawable.custom_icon_youtube,
            CustomIconRepository.bundledResourceFor("YouTube", "com.google.android.youtube.tv"),
        )
        assertEquals(
            R.drawable.custom_icon_downloader,
            CustomIconRepository.bundledResourceFor("Downloader", "com.esaba.downloader"),
        )
        assertEquals(
            R.drawable.custom_icon_play_store,
            CustomIconRepository.bundledResourceFor("Google Play Store", "com.android.vending"),
        )
        assertEquals(R.drawable.custom_icon_hulu, CustomIconRepository.bundledResourceFor("Hulu", "com.hulu.livingroomplus"))
        assertEquals(
            R.drawable.custom_icon_speed_test,
            CustomIconRepository.bundledResourceFor("Internet Speed Test", "com.rma.speedtesttv"),
        )
        assertEquals(
            R.drawable.custom_icon_play_games,
            CustomIconRepository.bundledResourceFor("Play Games", "com.google.android.play.games"),
        )
        assertEquals(
            R.drawable.custom_icon_play_movies,
            CustomIconRepository.bundledResourceFor("Play Movies & TV", "com.google.android.videos"),
        )
        assertEquals(
            R.drawable.custom_icon_surfshark,
            CustomIconRepository.bundledResourceFor("Surfshark", "com.surfshark.vpnclient.android"),
        )
        assertEquals(
            R.drawable.custom_icon_stremio,
            CustomIconRepository.bundledResourceFor("Stremio", "com.stremio.one"),
        )
        assertEquals(
            R.drawable.custom_icon_youtube_tv,
            CustomIconRepository.bundledResourceFor("YouTube TV", "com.google.android.youtube.tvunplugged"),
        )
        assertEquals(
            R.drawable.custom_icon_spotify,
            CustomIconRepository.bundledResourceFor("Spotify", "com.spotify.tv.android"),
        )
        assertNull(CustomIconRepository.bundledResourceFor("Plex", "com.plexapp.android"))
    }
}
