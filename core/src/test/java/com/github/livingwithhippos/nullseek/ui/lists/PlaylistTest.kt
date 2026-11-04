package com.github.livingwithhippos.nullseek.ui.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTest {
    private fun file(
        name: String,
        url: String = "https://x/$name",
        error: String? = null,
    ) = UnlockedFile(name, 1, if (error == null) url else "", null, null, error = error)

    @Test
    fun playlistKeepsTheListOrderAndSkipsFailures() {
        val playlist =
            buildM3u8Playlist(
                listOf(file("Ep10.mkv"), file("Ep2.mkv"), file("Ep3.mkv", error = "Infringing file (error 35)"), file("Ep1.mkv")),
                sortByName = false,
            )
        assertEquals(
            "#EXTM3U\n" +
                "#EXTINF:-1,Ep10.mkv\nhttps://x/Ep10.mkv\n" +
                "#EXTINF:-1,Ep2.mkv\nhttps://x/Ep2.mkv\n" +
                "#EXTINF:-1,Ep1.mkv\nhttps://x/Ep1.mkv\n",
            playlist,
        )
    }

    @Test
    fun playlistCanBeSortedNaturally() {
        val playlist = buildM3u8Playlist(listOf(file("Ep10.mkv"), file("Ep2.mkv"), file("Ep1.mkv")), sortByName = true)
        val names = playlist.lines().filter { it.startsWith("#EXTINF") }.map { it.substringAfter(',') }
        assertEquals(listOf("Ep1.mkv", "Ep2.mkv", "Ep10.mkv"), names)
    }

    @Test
    fun naturalOrderIgnoresCaseAndReadsNumbers() {
        val sorted = listOf("show S01E10", "Show S01E2", "SHOW S01E01", "show S02E01").sortedWith(naturalNameOrder)
        assertEquals(listOf("SHOW S01E01", "Show S01E2", "show S01E10", "show S02E01"), sorted)
        assertTrue(naturalNameOrder.compare("a", "B") < 0)
    }

    @Test
    fun playlistNameIsSafe() {
        assertEquals("Show_ S01_E02_.m3u8", playlistFileName("Show: S01/E02?"))
        assertEquals("playlist.m3u8", playlistFileName("  "))
    }
}
