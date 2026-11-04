package com.github.livingwithhippos.nullseek.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.nullseek.core.domain.DebridError
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.Page
import com.github.livingwithhippos.nullseek.core.domain.describe
import logcat.logcat

/**
 * Loads pages of torrents, web or usenet downloads from the active provider.
 * The key is the page number, [fetch] gets the offset and the limit.
 */
class DebridPagingSource<T : Any>(
    private val pageSize: Int,
    private val label: String,
    private val fetch: suspend (offset: Int, limit: Int) -> DebridResult<Page<T>>,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
        try {
            val pageNumber = params.key ?: 0
            val offset = pageNumber * pageSize
            when (val response = fetch(offset, pageSize)) {
                is DebridResult.Ok -> {
                    val page = response.value
                    logcat { "Loading $label page $pageNumber with offset $offset and limit $pageSize, found ${page.items.size}" }
                    LoadResult.Page(
                        data = page.items,
                        // Only paging forward.
                        prevKey = null,
                        nextKey = if (isEndOfList(offset, page, pageSize)) null else pageNumber + 1,
                    )
                }

                is DebridResult.Err -> {
                    if (response.error == DebridError.Unsupported) {
                        LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
                    } else {
                        logcat { "Failed to load $label: ${response.error}" }
                        LoadResult.Error(Exception(response.error.describe("Failed to load $label")))
                    }
                }
            }
        } catch (e: Exception) {
            logcat { "Error loading $label: $e" }
            LoadResult.Error(e)
        }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}

/** uses the total count when the provider sends it, a short page otherwise */
fun isEndOfList(
    offset: Int,
    page: Page<*>,
    pageSize: Int,
): Boolean =
    page.items.isEmpty() ||
        page.totalCount?.let { offset + page.items.size >= it } ?: (page.items.size < pageSize)
