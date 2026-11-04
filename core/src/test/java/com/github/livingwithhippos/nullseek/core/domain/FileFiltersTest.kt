package com.github.livingwithhippos.nullseek.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FileFiltersTest {
    private fun file(
        id: Int,
        path: String,
    ) = DebridFile(id = id, name = path, shortName = path.substringAfterLast('/'), size = 1, mimetype = null)

    // Real-Debrid files have no mimetype, only names
    private val files =
        listOf(
            file(1, "Show/Season 1/Episode.01.mkv"),
            file(2, "Show/Season 1/Episode.01.srt"),
            file(3, "Show/Soundtrack/theme.flac"),
            file(4, "Show/info.nfo"),
            file(5, "Show/Season 2/Episode.02.MP4"),
        )

    private fun List<DebridFile>.ids() = map { it.id }

    @Test
    fun noFilterKeepsEveryFile() {
        assertEquals(files, files.filterByType(emptySet()))
    }

    @Test
    fun typesAreFiltered() {
        assertEquals(listOf(1, 5), files.filterByType(setOf(FileTypeFilter.VIDEO)).ids())
        assertEquals(listOf(3), files.filterByType(setOf(FileTypeFilter.AUDIO)).ids())
        assertEquals(listOf(2), files.filterByType(setOf(FileTypeFilter.SUBTITLES)).ids())
        assertEquals(listOf(1, 2, 5), files.filterByType(setOf(FileTypeFilter.VIDEO, FileTypeFilter.SUBTITLES)).ids())
    }

    @Test
    fun availableTypesHideMissingOnes() {
        assertEquals(FileTypeFilter.entries.toList(), files.availableTypes())
        assertEquals(emptyList<FileTypeFilter>(), files.filter { it.id == 4 }.availableTypes())
    }

    @Test
    fun queryMatchesNamesAndFoldersIgnoringCase() {
        assertEquals(files, files.filterByQuery("  "))
        assertEquals(listOf(1, 2), files.filterByQuery("season 1").ids())
        assertEquals(listOf(3), files.filterByQuery("THEME").ids())
        assertEquals(listOf(5), files.filterByQuery("season 2").filterByType(setOf(FileTypeFilter.VIDEO)).ids())
    }
}
