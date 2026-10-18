package com.github.livingwithhippos.nullseek.ui.externalservices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.repository.IndexersRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IndexersUiState(
    val testingIndexerId: Long? = null,
    val isTestingInDialog: Boolean = false,
    val dialogTestResult: Result<Unit>? = null,
)

class IndexersViewModel(
    private val repository: IndexersRepository,
) : ViewModel() {
    val indexers: StateFlow<List<Indexer>> =
        repository.indexers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(IndexersUiState())
    val uiState: StateFlow<IndexersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExternalServicesEvent>()
    val events = _events.asSharedFlow()

    private fun launchCatching(
        errorPrefix: String,
        successMessage: String? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                block()
                successMessage?.let { _events.emit(ExternalServicesEvent.ShowMessage(it)) }
            } catch (e: Exception) {
                _events.emit(ExternalServicesEvent.ShowMessage("$errorPrefix: ${e.message}"))
            }
        }
    }

    fun addIndexer(indexer: Indexer) =
        launchCatching("Failed to add indexer", "Added ${indexer.name}") { repository.addIndexer(indexer) }

    fun updateIndexer(indexer: Indexer) =
        launchCatching("Failed to update indexer", "Updated ${indexer.type.displayName} indexer ${indexer.name}") {
            repository.updateIndexer(indexer)
        }

    fun deleteIndexer(indexer: Indexer) =
        launchCatching("Failed to delete indexer", "Deleted ${indexer.name}") { repository.deleteIndexer(indexer) }

    fun setActive(
        indexer: Indexer,
        active: Boolean,
    ) = launchCatching("Failed to update indexer") { repository.setActive(indexer.id, active) }

    fun testConnection(indexer: Indexer) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testingIndexerId = indexer.id)
            val result = repository.testConnection(indexer)
            _uiState.value = _uiState.value.copy(testingIndexerId = null)
            val message =
                result.fold(
                    onSuccess = { "Connected successfully to ${indexer.type.displayName} indexer \"${indexer.name}\"" },
                    onFailure = { "Connection failed: ${it.message}" },
                )
            _events.emit(ExternalServicesEvent.ShowMessage(message))
        }
    }

    fun testDialogConnection(indexer: Indexer) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingInDialog = true, dialogTestResult = null)
            val result = repository.testConnection(indexer)
            _uiState.value = _uiState.value.copy(isTestingInDialog = false, dialogTestResult = result)
        }
    }

    fun clearDialogTestResult() {
        _uiState.value = _uiState.value.copy(dialogTestResult = null)
    }
}
