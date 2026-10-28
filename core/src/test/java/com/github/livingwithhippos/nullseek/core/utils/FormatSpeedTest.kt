package com.github.livingwithhippos.nullseek.core.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatSpeedTest {
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `slow speeds keep one decimal in KB per second`() {
        assertEquals("0.0 KB/s", formatSpeed(0))
        assertEquals("0.5 KB/s", formatSpeed(512))
        assertEquals("9.5 KB/s", formatSpeed(9_728))
    }

    @Test
    fun `speeds below 1 MB per second are whole KB per second`() {
        assertEquals("10 KB/s", formatSpeed(10_240))
        assertEquals("1023 KB/s", formatSpeed(1_048_575))
    }

    @Test
    fun `speeds from 1 MB per second are MB per second`() {
        assertEquals("1.0 MB/s", formatSpeed(1_048_576))
        assertEquals("12.5 MB/s", formatSpeed(13_107_200))
    }

    @Test
    fun `negative speeds are treated as zero`() {
        assertEquals("0.0 KB/s", formatSpeed(-5))
    }
}
