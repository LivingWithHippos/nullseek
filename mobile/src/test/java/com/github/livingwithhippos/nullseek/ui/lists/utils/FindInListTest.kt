package com.github.livingwithhippos.nullseek.ui.lists.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FindInListTest {
    private val names = listOf("Ubuntu ISO", null, "debian", "UBUNTU server", "arch")

    @Test
    fun matchesAreCaseInsensitiveAndSkipNulls() {
        assertEquals(listOf(0, 3), findMatchIndices(names, "ubuntu"))
    }

    @Test
    fun blankQueryHasNoMatches() {
        assertEquals(emptyList<Int>(), findMatchIndices(names, "  "))
    }

    @Test
    fun noMatches() {
        assertEquals(emptyList<Int>(), findMatchIndices(names, "fedora"))
    }

    @Test
    fun positionWraps() {
        assertEquals(0, wrapMatchPosition(2, 1, 3))
        assertEquals(2, wrapMatchPosition(0, -1, 3))
        assertEquals(1, wrapMatchPosition(0, 1, 3))
        assertEquals(0, wrapMatchPosition(5, 1, 0))
    }
}
