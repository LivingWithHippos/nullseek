package com.github.livingwithhippos.nullseek.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.network.ApiResult
import logcat.logcat

class TorrentsPagingSource(
    val client: TorboxClient,
    val pageSize: Int = 100
) : PagingSource<Int, TorrentDto>() {
    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, TorrentDto> {
        try {
            // Start refresh at page 0 if undefined.
            val nextPageNumber = params.key ?: 0
            val nextOffset = nextPageNumber * pageSize
            val response = client.torrent.list(offset = nextOffset, limit = pageSize)
            if (response is ApiResult.Success) {
                val torrents = response.value.data ?: emptyList()
                logcat { "Loading torrents page $nextPageNumber with offset $nextOffset and limit $pageSize, found ${torrents.size}" }
                return LoadResult.Page(
                    data = torrents,
                    // todo: check if paging only forward is correct
                    prevKey = null, // Only paging forward.
                    nextKey = nextPageNumber + 1
                )
            } else {
                logcat { "Failed to load torrents: $response" }
                return LoadResult.Error(Exception("Failed to load torrents"))
            }
        } catch (e: Exception) {
            logcat { "Error loading torrents: $e" }
            // Handle errors in this block and return LoadResult.Error for
            // expected errors (such as a network failure).
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TorrentDto>): Int? {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
