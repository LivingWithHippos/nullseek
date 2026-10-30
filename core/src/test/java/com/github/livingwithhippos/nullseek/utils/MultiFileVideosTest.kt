package com.github.livingwithhippos.nullseek.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiFileVideosTest {
    private data class F(
        val name: String,
        val mime: String? = null,
    )

    private fun check(vararg files: F) = multiFileVideos(files.toList(), { it.name }, { it.mime })

    @Test
    fun `single file items get nothing`() {
        assertNull(check(F("movie.mkv")))
    }

    @Test
    fun `one video among other files is offered alone`() {
        assertEquals(MultiFileVideos.Single(F("movie.mkv")), check(F("movie.mkv"), F("movie.nfo"), F("movie.srt")))
    }

    @Test
    fun `several videos are counted`() {
        assertEquals(MultiFileVideos.Many(3), check(F("a.mkv"), F("b.mp4"), F("c", "video/x-matroska"), F("info.txt")))
    }

    @Test
    fun `items without videos get nothing`() {
        assertNull(check(F("song.flac"), F("cover.jpg")))
    }
}
