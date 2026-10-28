package com.github.livingwithhippos.nullseek.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import com.github.livingwithhippos.torbox.network.ApiResult
import logcat.logcat

class WebPagingSource(
    val client: TorboxClient,
    val pageSize: Int = DEFAULT_PAGING_LOAD_SIZE,
) : PagingSource<Int, WebDownloadDto>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WebDownloadDto> {
        try {
            val nextPageNumber = params.key ?: 0
            val nextOffset = nextPageNumber * pageSize
            val response = client.web.list(offset = nextOffset, limit = pageSize)
            if (response is ApiResult.Success) {
                val items = response.value.data ?: emptyList()
                val isEndOfList = items.size < pageSize
                logcat { "Loading web page $nextPageNumber with offset $nextOffset and limit $pageSize, found ${items.size}" }
                return LoadResult.Page(
                    data = items,
                    //  prevKey = if (nextPageNumber == 0) null else nextPageNumber - 1,
                    prevKey = null, // Only paging forward.
                    nextKey = if (isEndOfList) null else nextPageNumber + 1,
                )
            } else {
                logcat { "Failed to load web: $response" }
                return LoadResult.Error(Exception("Failed to load web"))
            }
        } catch (e: Exception) {
            logcat { "Error loading web: $e" }
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, WebDownloadDto>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
