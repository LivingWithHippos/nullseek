package com.github.livingwithhippos.nullseek.ui.lists.torrent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.livingwithhippos.nullseek.data.repository.TorrentsPagingSource
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.TorrentDto
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TorrentsViewModel(
    private val client: TorboxClient,
    val pageSize: Int = 100,
) : ViewModel() {
    // / todo: delete if unused
    val torrentsPagingFlow: Flow<PagingData<TorrentDto>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = pageSize,
                    enablePlaceholders = true,
                ),
            pagingSourceFactory = {
                // todo: inject?
                TorrentsPagingSource(client, pageSize)
            },
        ).flow.cachedIn(viewModelScope)
}
