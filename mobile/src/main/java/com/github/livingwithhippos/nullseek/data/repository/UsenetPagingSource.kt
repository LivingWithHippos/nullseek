package com.github.livingwithhippos.nullseek.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.network.ApiResult
import logcat.logcat

class UsenetPagingSource(
    val client: TorboxClient,
    val pageSize: Int = 100
) : PagingSource<Int, UsenetDownloadDto>() {
    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, UsenetDownloadDto> {
        try {
            // Start refresh at page 0 if undefined.
            val nextPageNumber = params.key ?: 0
            val nextOffset = nextPageNumber * pageSize
            val response = client.usenet.list(offset = nextOffset, limit = pageSize)
            if (response is ApiResult.Success) {
                val items = response.value.data ?: emptyList()
                val isEndOfList = items.size < pageSize
                logcat { "Loading usenet page $nextPageNumber with offset $nextOffset and limit $pageSize, found ${items.size}" }
                return LoadResult.Page(
                    data = items,
                    prevKey = null, // Only paging forward.
                    nextKey = if (isEndOfList) null else nextPageNumber + 1
                )
            } else {
                logcat { "Failed to load usenet: $response" }
                return LoadResult.Error(Exception("Failed to load usenet"))
            }
        } catch (e: Exception) {
            logcat { "Error loading usenet: $e" }
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UsenetDownloadDto>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}