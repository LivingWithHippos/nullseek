package com.github.livingwithhippos.nullseek.utils

import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WholeDownloadZipTest {
    private fun zipped(
        mode: DownloadZipMode,
        vararg names: String,
    ) = wholeDownloadIsZipped(mode, names.toList(), { it }, { null })

    @Test
    fun `several files are always zipped`() {
        DownloadZipMode.entries.forEach { assertTrue(zipped(it, "a.mkv", "a.srt")) }
    }

    @Test
    fun `a single file follows the zip setting`() {
        assertTrue(zipped(DownloadZipMode.ZIP, "movie.mkv"))
        assertFalse(zipped(DownloadZipMode.UNCOMPRESSED, "archive.rar"))
        // the dialog decides
        assertFalse(zipped(DownloadZipMode.ASK, "archive.rar"))
    }

    @Test
    fun `zip non media only zips files that are not media`() {
        assertFalse(zipped(DownloadZipMode.ZIP_NON_MEDIA, "movie.mkv"))
        assertFalse(zipped(DownloadZipMode.ZIP_NON_MEDIA, "subs.srt"))
        assertTrue(zipped(DownloadZipMode.ZIP_NON_MEDIA, "archive.rar"))
    }
}
