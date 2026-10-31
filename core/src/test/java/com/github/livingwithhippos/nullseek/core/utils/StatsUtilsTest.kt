package com.github.livingwithhippos.nullseek.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatsUtilsTest {
    @Test
    fun `month labels follow the locale`() {
        assertEquals("October 2026", formatBandwidthMonth("2026-10-01T00:00:00Z", Locale.ENGLISH))
        assertEquals("ottobre 2026", formatBandwidthMonth("2026-10-01T00:00:00Z", Locale.ITALIAN))
    }

    @Test
    fun `unparsable dates are shown as they are`() {
        assertEquals("not a date", formatBandwidthMonth("not a date", Locale.ENGLISH))
    }
}
