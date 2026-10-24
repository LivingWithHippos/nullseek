package com.github.livingwithhippos.nullseek.ui.search

import com.github.livingwithhippos.nullseek.core.data.ScrapedItem
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.core.data.stremio.AddonScrapedItem
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbTitleResult
import com.github.livingwithhippos.nullseek.core.data.stremio.SeriesEpisodeItem
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerSearchOutcome
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.repository.updateSearchHistoryOnAdd
import com.github.livingwithhippos.nullseek.data.repository.updateSearchHistoryOnRemove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSortTest {
    private val a = SearchResult("b", "i1", sizeBytes = 10, seeders = 1, magnetLink = "magnet:?xt=a")
    private val b = SearchResult("a", "i1", sizeBytes = 30, seeders = 9, magnetLink = "magnet:?xt=b")
    private val c = SearchResult("c", "i2", sizeBytes = 20, seeders = 5, magnetLink = "magnet:?xt=c")
    private val all = listOf(a, b, c)

    @Test
    fun sortsBySeedersDescending() {
        assertEquals(listOf(b, c, a), all.applySort(SearchSort(SearchSortOption.SEEDERS, false)))
    }

    @Test
    fun sortsByNameAscending() {
        assertEquals(listOf(b, a, c), all.applySort(SearchSort(SearchSortOption.NAME, true)))
    }

    @Test
    fun relevanceKeepsOrder() {
        assertEquals(all, all.applySort(SearchSort(SearchSortOption.RELEVANCE, true)))
    }

    @Test
    fun combinesInIndexerOrderAndKeepsFailures() {
        val i1 = Indexer(1, "One", IndexerType.JACKETT, "x")
        val i2 = Indexer(2, "Two", IndexerType.PROWLARR, "y")
        val i3 = Indexer(3, "Three", IndexerType.PROWLARR, "z")
        val progress =
            SearchProgress(
                "q",
                3,
                listOf(
                    IndexerSearchOutcome.Success(i2, listOf(c)),
                    IndexerSearchOutcome.Failure(i3, RuntimeException()),
                    IndexerSearchOutcome.Success(i1, listOf(a, b)),
                ),
                started = true,
            )
        val state = buildSearchState(progress, SearchSort(), listOf(i1, i2, i3))
        assertEquals(listOf(a, b, c), state.results)
        assertEquals(listOf("Three"), state.failedIndexers)
        assertEquals(SearchStatus.Results, state.status)
    }

    @Test
    fun searchModeDeterminesCanSearchAndSortsStremioStreams() {
        val addon =
            StremioAddonEntity(
                manifestUrl = "https://torrentio.strem.fun/manifest.json",
                id = "com.stremio.torrentio.addon",
                name = "Torrentio",
                description = "",
                version = "0.0.15",
                resources = "[]",
                types = "[\"movie\",\"series\"]",
                logo = null,
                background = null,
                isEnabled = true,
            )

        val indexerModeNoIndexers =
            buildSearchState(
                progress = SearchProgress(),
                sort = SearchSort(),
                active = emptyList(),
                searchMode = SearchMode.INDEXERS,
                activeAddons = listOf(addon),
            )
        assertFalse(indexerModeNoIndexers.canSearch)

        val stremioModeWithAddon =
            buildSearchState(
                progress = SearchProgress(),
                sort = SearchSort(),
                active = emptyList(),
                searchMode = SearchMode.STREMIO_ADDONS,
                activeAddons = listOf(addon),
                stremioState =
                    StremioSearchState(
                        stage = StremioStage.STREAMS,
                        selectedTitle = ImdbTitleResult("tt1375666", "Inception", "2010", "movie"),
                        streamResults = listOf(a, b, c),
                        sort = SearchSort(SearchSortOption.SIZE, ascending = false),
                    ),
            )
        assertTrue(stremioModeWithAddon.canSearch)
        assertEquals(listOf(b, c, a), stremioModeWithAddon.stremioState.streamResults)
    }

    @Test
    fun stremioStreamFilterBarFiltersAndRespectsVisibility() {
        val s1 = SearchResult("Inception.2010.2160p.REMUX.HEVC.mkv", "Torrentio", sizeBytes = 40_000, magnetLink = "magnet:?xt=1")
        val s2 = SearchResult("Inception.2010.1080p.BluRay.x264.mkv", "Torrentio", sizeBytes = 10_000, magnetLink = "magnet:?xt=2")
        val s3 = SearchResult("Inception.2010.1080p.WEB-DL.mp4", "Comet", sizeBytes = 8_000, magnetLink = "magnet:?xt=3")

        val stateWithFilter =
            StremioSearchState(
                stage = StremioStage.STREAMS,
                streamResults = listOf(s1, s2, s3),
                filterQuery = "1080p comet",
                showFilterBar = true,
            )
        assertEquals(listOf(s3), stateWithFilter.filteredStreamResults)

        // When showFilterBar is false, all results are returned regardless of filterQuery
        val stateHiddenFilter = stateWithFilter.copy(showFilterBar = false)
        assertEquals(listOf(s1, s2, s3), stateHiddenFilter.filteredStreamResults)
    }

    @Test
    fun stremioEpisodeSeasonFilteringOrdersSpecialsLast() {
        val episodes =
            listOf(
                SeriesEpisodeItem("tt1:0:1", 0, 1, "Special 1"),
                SeriesEpisodeItem("tt1:2:1", 2, 1, "S2E1"),
                SeriesEpisodeItem("tt1:1:2", 1, 2, "S1E2"),
                SeriesEpisodeItem("tt1:1:1", 1, 1, "S1E1"),
            )
        val stremioState =
            StremioSearchState(
                stage = StremioStage.EPISODES,
                selectedTitle = ImdbTitleResult("tt1", "Show", "2020", "series"),
                episodes = episodes,
                selectedSeason = 1,
            )
        assertEquals(listOf(1, 2, 0), stremioState.availableSeasons)
        assertEquals(listOf("tt1:1:1", "tt1:1:2"), stremioState.episodesForSelectedSeason.map { it.id })
    }

    @Test
    fun stremioAddonDisplayNameAndSearchResultFlag() {
        val defaultAddon =
            StremioAddonEntity(
                manifestUrl = "https://torrentio.strem.fun/manifest.json",
                id = "com.stremio.torrentio.addon",
                name = "Torrentio",
                description = "",
                version = "0.0.15",
                resources = "[]",
                types = "[\"movie\"]",
            )
        assertEquals("Torrentio", defaultAddon.displayName)

        val customNamedAddon = defaultAddon.copy(customName = "My Torrentio RD")
        assertEquals("My Torrentio RD", customNamedAddon.displayName)

        val blankCustomNamedAddon = defaultAddon.copy(customName = "   ")
        assertEquals("Torrentio", blankCustomNamedAddon.displayName)

        val scraped =
            AddonScrapedItem(
                scrapedItem =
                    ScrapedItem(
                        name = "Sample Movie 1080p",
                        link = null,
                        seeders = null,
                        leechers = null,
                        size = null,
                        addedDate = null,
                        parsedSize = null,
                        magnets = listOf("magnet:?xt=urn:btih:12345"),
                        torrents = emptyList(),
                        hosting = emptyList(),
                    ),
                addonName = customNamedAddon.displayName,
                addonId = customNamedAddon.id,
            )
        val result = scraped.toSearchResultOrNull()!!
        assertTrue(result.isFromStremioAddon)
        assertEquals("My Torrentio RD", result.indexerName)
        assertEquals(0, result.seeders)
        assertEquals(0, result.peers)
        assertEquals(0L, result.sizeBytes)
    }

    @Test
    fun searchHistoryDeduplicatesCaseInsensitivelyAndCapsSize() {
        var history = emptyList<String>()
        history = updateSearchHistoryOnAdd(history, "Inception", maxEntries = 3)
        history = updateSearchHistoryOnAdd(history, "Interstellar", maxEntries = 3)
        history = updateSearchHistoryOnAdd(history, "inception", maxEntries = 3)
        assertEquals(listOf("inception", "Interstellar"), history)

        history = updateSearchHistoryOnAdd(history, "Dune", maxEntries = 3)
        history = updateSearchHistoryOnAdd(history, "Arrival", maxEntries = 3)
        assertEquals(listOf("Arrival", "Dune", "inception"), history)

        history = updateSearchHistoryOnRemove(history, "Dune")
        assertEquals(listOf("Arrival", "inception"), history)
    }
}
