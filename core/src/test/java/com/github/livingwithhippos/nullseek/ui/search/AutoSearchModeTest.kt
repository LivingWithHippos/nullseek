package com.github.livingwithhippos.nullseek.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoSearchModeTest {
    @Test
    fun switchesToAddonsWithoutIndexers() {
        assertEquals(SearchMode.STREMIO_ADDONS, autoSearchMode(SearchMode.INDEXERS, enabledIndexers = 0, enabledAddons = 2))
    }

    @Test
    fun switchesToIndexersWithoutAddons() {
        assertEquals(SearchMode.INDEXERS, autoSearchMode(SearchMode.STREMIO_ADDONS, enabledIndexers = 3, enabledAddons = 0))
    }

    @Test
    fun keepsTheModeWhenBothHaveSources() {
        assertNull(autoSearchMode(SearchMode.INDEXERS, enabledIndexers = 1, enabledAddons = 1))
        assertNull(autoSearchMode(SearchMode.STREMIO_ADDONS, enabledIndexers = 1, enabledAddons = 1))
    }

    @Test
    fun keepsTheModeWhenNothingIsSet() {
        assertNull(autoSearchMode(SearchMode.INDEXERS, enabledIndexers = 0, enabledAddons = 0))
        assertNull(autoSearchMode(SearchMode.STREMIO_ADDONS, enabledIndexers = 0, enabledAddons = 0))
    }

    @Test
    fun keepsTheModeThatHasSources() {
        assertNull(autoSearchMode(SearchMode.INDEXERS, enabledIndexers = 2, enabledAddons = 0))
        assertNull(autoSearchMode(SearchMode.STREMIO_ADDONS, enabledIndexers = 0, enabledAddons = 2))
    }
}
