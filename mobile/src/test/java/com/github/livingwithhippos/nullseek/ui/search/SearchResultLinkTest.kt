package com.github.livingwithhippos.nullseek.ui.search

import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerSearchOutcome
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.model.hasDownloadLink
import com.github.livingwithhippos.nullseek.data.model.isDirectStreamResult
import com.github.livingwithhippos.nullseek.data.model.preferredLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultLinkTest {
    private val both = SearchResult("both", "i", magnetLink = "magnet:?xt=a", downloadLink = "http://t/1.torrent")
    private val torrent = SearchResult("torrent", "i", downloadLink = "http://t/2.torrent")
    private val directStream =
        SearchResult("Movie.2024.1080p.mkv\n⚡ TorBox", "Stremio Addons", downloadLink = "https://stream/1", isDirectStream = true)
    private val blank = SearchResult("blank", "i", magnetLink = " ", downloadLink = "")
    private val none = SearchResult("none", "i")

    @Test
    fun prefersMagnet() = assertEquals("magnet:?xt=a", both.preferredLink())

    @Test
    fun fallsBackToTorrent() {
        assertEquals("http://t/2.torrent", torrent.preferredLink())
        assertEquals("http://t/2.torrent", SearchResult("x", "i", magnetLink = " ", downloadLink = "http://t/2.torrent").preferredLink())
    }

    @Test
    fun detectsDirectStreamResult() {
        assertTrue(directStream.isDirectStreamResult())
        assertFalse(torrent.isDirectStreamResult())
        assertFalse(both.isDirectStreamResult())
        assertEquals("Movie.2024.1080p.mkv", resolveStreamFileName(directStream.title, directStream.downloadLink.orEmpty()))
    }

    @Test
    fun noLink() {
        assertNull(blank.preferredLink())
        assertNull(none.preferredLink())
        assertFalse(blank.hasDownloadLink())
        assertFalse(none.hasDownloadLink())
        assertTrue(both.hasDownloadLink())
        assertTrue(torrent.hasDownloadLink())
    }

    @Test
    fun stateHidesResultsWithoutLinks() {
        val i1 = Indexer(1, "One", IndexerType.JACKETT, "x")
        val progress =
            SearchProgress("q", 1, listOf(IndexerSearchOutcome.Success(i1, listOf(none, both, blank, torrent))), started = true)
        val state = buildSearchState(progress, SearchSort(), listOf(i1))
        assertEquals(listOf(both, torrent), state.results)

        val empty = SearchProgress("q", 1, listOf(IndexerSearchOutcome.Success(i1, listOf(none))), started = true)
        assertTrue(buildSearchState(empty, SearchSort(), listOf(i1)).results.isEmpty())
    }
}
