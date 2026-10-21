package com.github.livingwithhippos.nullseek.ui.search

import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerSearchOutcome
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchSortTest {
    private val a = SearchResult("b", "i1", sizeBytes = 10, seeders = 1)
    private val b = SearchResult("a", "i1", sizeBytes = 30, seeders = 9)
    private val c = SearchResult("c", "i2", sizeBytes = 20, seeders = 5)
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
}
