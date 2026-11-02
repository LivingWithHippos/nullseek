package com.github.livingwithhippos.nullseek.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridDownload
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridService
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.Page
import com.github.livingwithhippos.nullseek.data.repository.DebridPagingSource
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class ListsViewModel(
    private val debridProvider: DebridServiceProvider,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    // one per list: a quick repeated pull to refresh reads TorBox's cached list instead of bypassing it
    private val torrentsGate = FreshLoadGate()
    private val webGate = FreshLoadGate()
    private val usenetGate = FreshLoadGate()

    val capabilities: StateFlow<DebridCapabilities?> =
        debridProvider.active
            .map { it?.capabilities }
            .stateIn(viewModelScope, SharingStarted.Eagerly, debridProvider.active.value?.capabilities)

    // the lists are rebuilt when the page size or the active account changes
    private val pagingKeys =
        combine(
            preferencesRepository.pagingLoadSize.distinctUntilChanged(),
            debridProvider.active.map { it?.provider?.id to it?.provider?.token }.distinctUntilChanged(),
        ) { pageSize, _ -> pageSize to debridProvider.current }

    private fun <T : Any> pager(
        label: String,
        fetch: suspend DebridService.(offset: Int, limit: Int, bypassCache: Boolean) -> DebridResult<Page<T>>,
        gate: FreshLoadGate,
    ): Flow<PagingData<T>> =
        pagingKeys
            .flatMapLatest { (requestedSize, service) ->
                val pageSize = service.capabilities.maxPageSize?.let { requestedSize.coerceAtMost(it) } ?: requestedSize
                Pager(
                    config =
                        PagingConfig(
                            pageSize = pageSize,
                            enablePlaceholders = false,
                        ),
                    pagingSourceFactory = {
                        val bypassCache = gate.tryFreshLoad()
                        DebridPagingSource(pageSize, label) { offset, limit ->
                            service.fetch(offset, limit, bypassCache)
                        }
                    },
                ).flow
            }.cachedIn(viewModelScope)

    val torrentsPagingFlow: Flow<PagingData<DebridTorrent>> =
        pager("torrents", { offset, limit, bypass -> listTorrents(offset, limit, bypass) }, torrentsGate)

    val webPagingFlow: Flow<PagingData<DebridDownload>> =
        pager("web downloads", { offset, limit, bypass -> listWeb(offset, limit, bypass) }, webGate)

    val usenetPagingFlow: Flow<PagingData<DebridDownload>> =
        pager("usenet downloads", { offset, limit, bypass -> listUsenet(offset, limit, bypass) }, usenetGate)
}
