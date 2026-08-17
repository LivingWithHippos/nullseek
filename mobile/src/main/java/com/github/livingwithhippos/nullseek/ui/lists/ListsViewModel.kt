package com.github.livingwithhippos.nullseek.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.livingwithhippos.nullseek.data.repository.TorrentsPagingSource
import com.github.livingwithhippos.nullseek.data.repository.UsenetPagingSource
import com.github.livingwithhippos.nullseek.data.repository.WebPagingSource
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ListsViewModel(
    private val client: TorboxClient,
    val pageSize: Int = 100,
) : ViewModel() {
    // todo: inject the sources

    val torrentsPagingFlow: Flow<PagingData<TorrentDto>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = pageSize,
                    enablePlaceholders = true,
                ),
            pagingSourceFactory = {
                TorrentsPagingSource(client, pageSize)
            },
        ).flow.cachedIn(viewModelScope)

    val webPagingFlow: Flow<PagingData<WebDownloadDto>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = pageSize,
                    enablePlaceholders = true,
                ),
            pagingSourceFactory = {
                WebPagingSource(client, pageSize)
            },
        ).flow.cachedIn(viewModelScope)

    val usenetPagingFlow: Flow<PagingData<UsenetDownloadDto>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = pageSize,
                    enablePlaceholders = true,
                ),
            pagingSourceFactory = {
                UsenetPagingSource(client, pageSize)
            },
        ).flow.cachedIn(viewModelScope)
}
