package com.github.livingwithhippos.nullseek.ui.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshLoadGateTest {
    private var clock = 0L
    private val gate = FreshLoadGate(intervalMs = 10_000L, now = { clock })

    @Test
    fun `the first load is fresh`() {
        assertTrue(gate.tryFreshLoad())
    }

    @Test
    fun `a load within the interval uses the cache`() {
        gate.tryFreshLoad()
        clock = 5_000L
        assertFalse(gate.tryFreshLoad())
    }

    @Test
    fun `a load after the interval is fresh again`() {
        gate.tryFreshLoad()
        clock = 10_000L
        assertTrue(gate.tryFreshLoad())
    }

    @Test
    fun `the interval is measured from the last fresh load`() {
        // pulling every 5 seconds: fresh, cached, fresh, cached
        val results =
            (0..3).map { step ->
                clock = step * 5_000L
                gate.tryFreshLoad()
            }
        assertEquals(listOf(true, false, true, false), results)
    }
}
