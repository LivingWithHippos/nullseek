package com.github.livingwithhippos.nullseek.data.model

import com.github.livingwithhippos.nullseek.data.remote.parseJackettResults
import com.github.livingwithhippos.nullseek.data.remote.parseProwlarrResults
import com.github.livingwithhippos.nullseek.ui.search.SearchProgress
import com.github.livingwithhippos.nullseek.ui.search.SearchSort
import com.github.livingwithhippos.nullseek.ui.search.buildSearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultCacheTest {
    private val hash = "c9e15763f722f23e98a29decdfae341b98d53056"
    private val magnetResult = SearchResult("m", "i", magnetLink = "magnet:?xt=urn:btih:${hash.uppercase()}&dn=m")
    private val torrentFileResult = SearchResult("t", "i", downloadLink = "http://jackett/dl/1.torrent", infoHash = hash)
    private val directResult =
        SearchResult(
            "d",
            "addon",
            downloadLink = "https://cdn.example.com/files/movie.mkv?token=abc",
            isDirectStream = true,
            isFromStremioAddon = true,
        )

    @Test
    fun `torrent hash comes from the indexer or the magnet`() {
        assertEquals(hash, magnetResult.torrentHash())
        assertEquals(hash, torrentFileResult.torrentHash())
        assertNull(directResult.torrentHash())
    }

    @Test
    fun `results are marked from the cached set`() {
        val cached = setOf(hash)
        assertTrue(magnetResult.markCached(cached).isCached)
        assertTrue(torrentFileResult.markCached(cached).isCached)
        // direct links have no torrent hash, so they are never marked
        assertFalse(directResult.markCached(cached).isCached)
        assertFalse(magnetResult.markCached(emptySet()).isCached)
        assertFalse(magnetResult.markCached(setOf("other")).isCached)
    }

    @Test
    fun `search state marks cached indexer results`() {
        val indexer = Indexer(id = 1, name = "idx", type = IndexerType.PROWLARR, address = "http://x", apiToken = "k", active = true)
        val progress =
            SearchProgress(
                query = "q",
                total = 1,
                outcomes =
                    listOf(
                        IndexerSearchOutcome.Success(
                            indexer,
                            listOf(magnetResult, SearchResult("x", "i", magnetLink = "magnet:?xt=urn:btih:${"a".repeat(40)}")),
                        ),
                    ),
                started = true,
            )
        val state = buildSearchState(progress, SearchSort(), listOf(indexer), cached = setOf(hash))
        assertEquals(listOf(true, false), state.results.map { it.isCached })
    }

    @Test
    fun `indexer info hashes are parsed`() {
        val jackett = parseJackettResults("""{"Results":[{"Title":"A","Link":"http://l","InfoHash":"$hash"}]}""", "f")
        assertEquals(hash, jackett[0].infoHash)
        val prowlarr = parseProwlarrResults("""[{"title":"B","downloadUrl":"http://dl","infoHash":"$hash"}]""", "f")
        assertEquals(hash, prowlarr[0].infoHash)
    }
}
