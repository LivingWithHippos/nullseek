package com.github.livingwithhippos.nullseek.ui.lists

/** Minimum time between two loads that bypass TorBox's list cache. */
internal const val FRESH_LOAD_INTERVAL_MS = 7_000L

/**
 * Allows a cache-bypassing load at most once every [intervalMs].
 * Measured from the last fresh load, so pulling to refresh often still gets fresh data regularly.
 */
internal class FreshLoadGate(
    private val intervalMs: Long = FRESH_LOAD_INTERVAL_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastFreshAt: Long? = null

    /** True if this load may bypass the cache; records it as the latest fresh load. */
    @Synchronized
    fun tryFreshLoad(): Boolean {
        val time = now()
        val last = lastFreshAt
        if (last != null && time - last < intervalMs) return false
        lastFreshAt = time
        return true
    }
}
