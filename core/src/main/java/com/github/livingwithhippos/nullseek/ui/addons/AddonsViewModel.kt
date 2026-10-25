package com.github.livingwithhippos.nullseek.ui.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.core.data.stremio.AddonManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddonsUiState(
    val addons: List<StremioAddonEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val installSuccess: Boolean = false,
    val message: String? = null,
)

class AddonsViewModel(
    private val addonManager: AddonManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    val addons: StateFlow<List<StremioAddonEntity>> =
        addonManager
            .getAllAddons()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    init {
        loadAddons()
    }

    private fun loadAddons() {
        viewModelScope.launch {
            addonManager
                .getAllAddons()
                .collect { addonList ->
                    _uiState.value =
                        _uiState.value.copy(
                            addons = addonList,
                            isLoading = false,
                        )
                }
        }
    }

    fun installAddon(
        manifestUrl: String,
        customName: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, installSuccess = false)

            addonManager
                .installAddon(manifestUrl, customName)
                .onSuccess { addon ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            installSuccess = true,
                            message = "Added ${addon.displayName}",
                        )
                }.onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to install addon",
                        )
                }
        }
    }

    fun updateAddonDetails(
        addon: StremioAddonEntity,
        customName: String?,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            val resolvedName = customName?.trim()?.takeIf { it.isNotBlank() } ?: addon.name
            addonManager
                .updateAddonDetails(addon.manifestUrl, resolvedName, enabled)
                .onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            message = "Updated $resolvedName",
                        )
                }.onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "Failed to update addon",
                        )
                }
        }
    }

    fun uninstallAddon(
        manifestUrl: String,
        name: String? = null,
    ) {
        viewModelScope.launch {
            addonManager
                .uninstallAddon(manifestUrl)
                .onSuccess {
                    if (!name.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(message = "Deleted $name")
                    }
                }
        }
    }

    fun toggleAddonEnabled(
        manifestUrl: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            addonManager.setAddonEnabled(manifestUrl, enabled)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearInstallSuccess() {
        _uiState.value = _uiState.value.copy(installSuccess = false)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
