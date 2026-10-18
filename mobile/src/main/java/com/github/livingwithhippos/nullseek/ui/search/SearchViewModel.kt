package com.github.livingwithhippos.nullseek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.repository.IndexersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

enum class SearchSortOption { RELEVANCE, SIZE, SEEDERS, PEERS, AGE, NAME }

data class SearchSort(
    val option: SearchSortOption = SearchSortOption.RELEVANCE,
    val ascending: Boolean = false,
)

data class SearchUiState(
    val sort: SearchSort = SearchSort(),
    val activeIndexers: List<Indexer> = emptyList(),
) {
    val hasActiveIndexers: Boolean get() = activeIndexers.isNotEmpty()
}

fun List<Indexer>.activeOnly(): List<Indexer> = filter { it.active }

@KoinViewModel
class SearchViewModel(
    indexersRepository: IndexersRepository,
) : ViewModel() {
    private val sort = MutableStateFlow(SearchSort())

    val uiState: StateFlow<SearchUiState> =
        combine(sort, indexersRepository.indexers.map { it.activeOnly() }) { s, active ->
            SearchUiState(s, active)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setSortOption(option: SearchSortOption) = sort.update { it.copy(option = option) }

    fun setAscending(ascending: Boolean) = sort.update { it.copy(ascending = ascending) }

    // todo: Search execution is not implemented yet.
    fun onSearch(query: String) {

    }
}
