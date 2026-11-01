package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.logcat
import java.util.Locale

/**
 * Asks the active provider which torrent hashes are cached (TorBox only, through the POST batch endpoint).
 * Answers are kept for an hour, like TorBox does, so repeated searches don't ask again.
 */
class CacheChecker(
    private val debridProvider: DebridServiceProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private class Entry(
        val cached: Boolean,
        val checkedAt: Long,
    )

    private class Store {
        val entries = HashMap<String, Entry>()
        val mutex = Mutex()
    }

    // one per provider, so answers don't leak across an account switch
    private val torrentStores = ProviderKind.entries.associateWith { Store() }

    /** true when the active provider can check the cache */
    val isSupported: Boolean get() = debridProvider.capabilities.cacheCheck

    /** The subset of [hashes] (lowercase hex info hashes) that is cached on the active provider. */
    suspend fun cachedTorrents(hashes: Collection<String>): Set<String> {
        val service = debridProvider.current
        if (!service.capabilities.cacheCheck) return emptySet()
        return cachedAmong(hashes, torrentStores.getValue(service.kind), "torrent") { service.cachedHashes(it) }
    }

    private suspend fun cachedAmong(
        hashes: Collection<String>,
        store: Store,
        label: String,
        request: suspend (List<String>) -> DebridResult<Set<String>>,
    ): Set<String> {
        val wanted = hashes.map { it.lowercase(Locale.ROOT) }.toSet()
        if (wanted.isEmpty()) return emptySet()

        store.mutex.withLock {
            val cache = store.entries
            val time = now()
            val missing = wanted.filter { hash -> cache[hash]?.let { time - it.checkedAt < CACHE_TTL_MS } != true }
            missing.chunked(BATCH_SIZE).forEach { chunk ->
                when (val result = withContext(Dispatchers.IO) { request(chunk) }) {
                    is DebridResult.Ok -> {
                        val cached = result.value.map { it.lowercase(Locale.ROOT) }.toSet()
                        chunk.forEach { cache[it] = Entry(it in cached, time) }
                        logcat { "$label cache: ${chunk.count { it in cached }} of ${chunk.size} cached" }
                    }

                    else -> {
                        logcat { "$label cache check failed: $result" }
                    }
                }
            }
            return wanted.filterTo(HashSet()) { cache[it]?.cached == true }
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val CACHE_TTL_MS = 60 * 60 * 1000L
    }
}
