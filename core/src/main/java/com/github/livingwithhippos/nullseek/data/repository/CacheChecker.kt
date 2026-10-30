package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.ApiResponseListDto
import com.github.livingwithhippos.torbox.model.CacheDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.logcat
import java.util.Locale

/**
 * Asks TorBox which torrent hashes are cached, through the POST batch endpoint.
 * Answers are kept for an hour, like TorBox does, so repeated searches don't ask again.
 */
class CacheChecker(
    private val client: TorboxClient,
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

    private val torrentStore = Store()

    /** The subset of [hashes] (lowercase hex info hashes) that is cached on TorBox. */
    suspend fun cachedTorrents(hashes: Collection<String>): Set<String> =
        cachedAmong(hashes, torrentStore, "torrent") { client.torrent.listAvailabilityBatch(it) }

    private suspend fun cachedAmong(
        hashes: Collection<String>,
        store: Store,
        label: String,
        request: suspend (List<String>) -> ApiResult<ApiResponseListDto<CacheDto>>,
    ): Set<String> {
        val wanted = hashes.map { it.lowercase(Locale.ROOT) }.toSet()
        if (wanted.isEmpty()) return emptySet()

        store.mutex.withLock {
            val cache = store.entries
            val time = now()
            val missing = wanted.filter { hash -> cache[hash]?.let { time - it.checkedAt < CACHE_TTL_MS } != true }
            missing.chunked(BATCH_SIZE).forEach { chunk ->
                when (val result = withContext(Dispatchers.IO) { request(chunk) }) {
                    is ApiResult.Success -> {
                        val cached =
                            result.value.data
                                .orEmpty()
                                .map { it.hash.lowercase(Locale.ROOT) }
                                .toSet()
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
