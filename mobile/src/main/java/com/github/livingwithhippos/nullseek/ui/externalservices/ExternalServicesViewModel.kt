package com.github.livingwithhippos.nullseek.ui.externalservices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExternalServicesEvent {
    data class ShowMessage(
        val message: String,
    ) : ExternalServicesEvent
}

data class ExternalServicesUiState(
    val testingServiceId: Long? = null,
    val isTestingInDialog: Boolean = false,
    val dialogTestResult: Result<Unit>? = null,
)

class ExternalServicesViewModel(
    private val repository: ExternalServicesRepository,
) : ViewModel() {
    val services: StateFlow<List<ExternalService>> =
        repository.allServices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(ExternalServicesUiState())
    val uiState: StateFlow<ExternalServicesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExternalServicesEvent>()
    val events = _events.asSharedFlow()

    fun addService(service: ExternalService) {
        viewModelScope.launch {
            try {
                repository.addService(service)
                _events.emit(ExternalServicesEvent.ShowMessage("Added ${service.name}"))
            } catch (e: Exception) {
                _events.emit(ExternalServicesEvent.ShowMessage("Failed to add service: ${e.message}"))
            }
        }
    }

    fun updateService(service: ExternalService) {
        viewModelScope.launch {
            try {
                repository.updateService(service)
                _events.emit(ExternalServicesEvent.ShowMessage("Updated ${service.name}"))
            } catch (e: Exception) {
                _events.emit(ExternalServicesEvent.ShowMessage("Failed to update service: ${e.message}"))
            }
        }
    }

    fun deleteService(
        id: Long,
        name: String,
    ) {
        viewModelScope.launch {
            try {
                repository.deleteService(id)
                _events.emit(ExternalServicesEvent.ShowMessage("Deleted $name"))
            } catch (e: Exception) {
                _events.emit(ExternalServicesEvent.ShowMessage("Failed to delete service: ${e.message}"))
            }
        }
    }

    fun setDefault(
        id: Long,
        category: ExternalServiceCategory,
        name: String,
    ) {
        viewModelScope.launch {
            try {
                repository.setDefault(id, category)
                _events.emit(ExternalServicesEvent.ShowMessage("Set $name as default"))
            } catch (e: Exception) {
                _events.emit(ExternalServicesEvent.ShowMessage("Failed to set default: ${e.message}"))
            }
        }
    }

    fun testConnection(service: ExternalService) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testingServiceId = service.id)
            val result = repository.testConnection(service)
            _uiState.value = _uiState.value.copy(testingServiceId = null)

            result.fold(
                onSuccess = {
                    _events.emit(ExternalServicesEvent.ShowMessage("Connected successfully to ${service.type.displayName} service \"${service.name}\""))
                },
                onFailure = { error ->
                    _events.emit(ExternalServicesEvent.ShowMessage("Connection failed: ${error.message}"))
                },
            )
        }
    }

    fun testDialogConnection(service: ExternalService) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingInDialog = true, dialogTestResult = null)
            val result = repository.testConnection(service)
            _uiState.value = _uiState.value.copy(isTestingInDialog = false, dialogTestResult = result)
        }
    }

    fun clearDialogTestResult() {
        _uiState.value = _uiState.value.copy(dialogTestResult = null)
    }
}
