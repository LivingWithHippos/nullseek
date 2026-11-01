package com.github.livingwithhippos.nullseek.core.domain.realdebrid

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps requests under [maxRequests] per [windowMs], Real-Debrid allows 250 per minute.
 * Callers wait when the window is full.
 */
class RateLimiter(
    private val maxRequests: Int = 200,
    private val windowMs: Long = 60_000,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val timestamps = ArrayDeque<Long>()

    suspend fun acquire() {
        while (true) {
            val wait =
                mutex.withLock {
                    val time = now()
                    while (timestamps.isNotEmpty() && time - timestamps.first() >= windowMs) {
                        timestamps.removeFirst()
                    }
                    if (timestamps.size < maxRequests) {
                        timestamps.addLast(time)
                        0L
                    } else {
                        windowMs - (time - timestamps.first())
                    }
                }
            if (wait <= 0) return
            delay(wait)
        }
    }
}
