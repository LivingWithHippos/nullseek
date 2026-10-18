package com.github.livingwithhippos.nullseek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.repository.DownloadCreator
import com.github.livingwithhippos.nullseek.data.repository.IndexersRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import com.github.livingwithhippos.nullseek.data.model.IndexerSearchOutcome
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.model.hasDownloadLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

enum class SearchSortOption { RELEVANCE, SIZE, SEEDERS, PEERS, AGE, NAME }

data class SearchSort(
    val option: SearchSortOption = SearchSortOption.RELEVANCE,
    val ascending: Boolean = false,
)

sealed interface SearchStatus {
    data object Idle : SearchStatus

    data class Loading(val completed: Int, val total: Int) : SearchStatus

    data object Results : SearchStatus
}

/** Raw, unsorted progress of the current search. */
data class SearchProgress(
    val query: String = "",
    val total: Int = 0,
    val outcomes: List<IndexerSearchOutcome> = emptyList(),
    val started: Boolean = false,
) {
    val completed: Int get() = outcomes.size
    val isLoading: Boolean get() = started && completed < total
}

data class SearchUiState(
    val sort: SearchSort = SearchSort(),
    val activeIndexers: List<Indexer> = emptyList(),
    val status: SearchStatus = SearchStatus.Idle,
    val results: List<SearchResult> = emptyList(),
    val failedIndexers: List<String> = emptyList(),
) {
    val hasActiveIndexers: Boolean get() = activeIndexers.isNotEmpty()
}

fun List<Indexer>.activeOnly(): List<Indexer> = filter { it.active }

/** Relevance keeps the indexers order, the direction only applies to the other options. */
fun List<SearchResult>.applySort(sort: SearchSort): List<SearchResult> {
    val sorted =
        when (sort.option) {
            SearchSortOption.RELEVANCE -> return this
            SearchSortOption.SIZE -> sortedBy { it.sizeBytes }
            SearchSortOption.SEEDERS -> sortedBy { it.seeders }
            SearchSortOption.PEERS -> sortedBy { it.peers }
            SearchSortOption.AGE -> sortedBy { it.publishedAt ?: 0L }
            SearchSortOption.NAME -> sortedBy { it.title.lowercase() }
        }
    return if (sort.ascending) sorted else sorted.reversed()
}

/** Combines the per-indexer outcomes, in indexers order, into a single sorted state. */
fun buildSearchState(
    progress: SearchProgress,
    sort: SearchSort,
    active: List<Indexer>,
): SearchUiState {
    val order = progress.outcomes.sortedBy { o -> active.indexOfFirst { it.id == o.indexer.id }.let { if (it < 0) Int.MAX_VALUE else it } }
    val results =
        order.filterIsInstance<IndexerSearchOutcome.Success>().flatMap { it.results }.filter { it.hasDownloadLink() }.applySort(sort)
    val failed = order.filterIsInstance<IndexerSearchOutcome.Failure>().map { it.indexer.name }
    val status =
        when {
            !progress.started -> SearchStatus.Idle
            progress.isLoading -> SearchStatus.Loading(progress.completed, progress.total)
            else -> SearchStatus.Results
        }
    return SearchUiState(sort, active, status, results, failed)
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@KoinViewModel
class SearchViewModel(
    private val indexersRepository: IndexersRepository,
    private val preferencesRepository: PreferencesRepository,
    private val downloadCreator: DownloadCreator,
) : ViewModel() {
    val autoAddDownload: StateFlow<Boolean> =
        preferencesRepository.searchAutoAddDownload.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )

    private val _addingDownload = MutableStateFlow(false)
    val addingDownload: StateFlow<Boolean> = _addingDownload

    private val _downloadEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /** Emits true when the download was added, false on failure. */
    val downloadEvents: SharedFlow<Boolean> = _downloadEvents

    private val sort = MutableStateFlow(SearchSort())
    private val progress = MutableStateFlow(SearchProgress())
    private val _selectedResult = MutableStateFlow<SearchResult?>(null)
    val selectedResult: StateFlow<SearchResult?> = _selectedResult
    private val errorsDismissed = MutableStateFlow(false)
    private val submissions =
        MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val uiState: StateFlow<SearchUiState> =
        combine(
            sort,
            indexersRepository.indexers.map { it.activeOnly() },
            progress,
            errorsDismissed,
        ) { s, active, p, dismissed ->
            val state = buildSearchState(p, s, active)
            if (dismissed) state.copy(failedIndexers = emptyList()) else state
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        // Bursts of submits are coalesced; flatMapLatest cancels the in-flight search (and its HTTP calls).
        submissions
            .debounce(SEARCH_DEBOUNCE_MS.milliseconds)
            .filter { it.isNotBlank() }
            .flatMapLatest { query -> searchFlow(query) }
            .onEach { progress.value = it }
            .launchIn(viewModelScope)
    }

    private fun searchFlow(query: String) =
        flow {
            val active = indexersRepository.getAll().activeOnly()
            if (active.isEmpty()) return@flow
            errorsDismissed.value = false
            var current = SearchProgress(query, active.size, emptyList(), started = true)
            emit(current)
            indexersRepository.search(active, query).collect { outcome ->
                current = current.copy(outcomes = current.outcomes + outcome)
                emit(current)
            }
        }

    fun setSortOption(option: SearchSortOption) = sort.update { it.copy(option = option) }

    fun setAscending(ascending: Boolean) = sort.update { it.copy(ascending = ascending) }

    fun selectResult(result: SearchResult) {
        _selectedResult.value = result
    }

    fun closeDetails() {
        _selectedResult.value = null
    }

    fun addDownload(link: String) {
        if (!_addingDownload.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val result = runCatching { downloadCreator.createFromLink(link) }.getOrNull()
            _addingDownload.value = false
            _downloadEvents.tryEmit(result != null && result !is ApiResult.Failure)
        }
    }

    fun dismissErrors() {
        errorsDismissed.value = true
    }

    fun onSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) submissions.tryEmit(trimmed)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
