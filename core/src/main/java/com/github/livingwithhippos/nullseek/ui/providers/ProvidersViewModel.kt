package com.github.livingwithhippos.nullseek.ui.providers

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProvidersEvent {
    data class ShowMessage(
        @StringRes val resId: Int,
        val arg: String? = null,
    ) : ProvidersEvent

    /** the active provider (or its token) changed, the app state must be reloaded */
    data object ActiveChanged : ProvidersEvent
}

data class ProvidersUiState(
    val validating: Boolean = false,
    val tokenError: Boolean = false,
)

class ProvidersViewModel(
    private val repository: NullProviderRepository,
    private val debridProvider: DebridServiceProvider,
) : ViewModel() {
    val providers: StateFlow<List<NullProvider>> =
        repository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProvidersEvent>()
    val events = _events.asSharedFlow()

    fun clearError() {
        _uiState.value = _uiState.value.copy(tokenError = false)
    }

    private suspend fun isTokenValid(
        kind: ProviderKind,
        token: String,
    ): Boolean {
        _uiState.value = ProvidersUiState(validating = true)
        val valid =
            try {
                debridProvider.newTemp(kind, token).user() is DebridResult.Ok
            } catch (e: Exception) {
                false
            }
        _uiState.value = ProvidersUiState(tokenError = !valid)
        return valid
    }

    /** @param onDone called with true when the provider was added */
    fun add(
        kind: ProviderKind,
        name: String,
        token: String,
        onDone: (Boolean) -> Unit,
    ) {
        val cleanName = name.trim()
        val cleanToken = token.trim()
        if (cleanName.isEmpty() || cleanToken.isEmpty()) return
        viewModelScope.launch {
            if (!isTokenValid(kind, cleanToken)) {
                onDone(false)
                return@launch
            }
            val netProvider = NetProvider.of(kind)
            val hasActive = repository.getActive() != null
            val provider =
                NullProvider(
                    type = netProvider.type,
                    name = cleanName,
                    token = cleanToken,
                    active = !hasActive,
                )
            if (hasActive) {
                repository.insert(provider)
            } else {
                repository.insertActive(provider)
                _events.emit(ProvidersEvent.ActiveChanged)
            }
            _events.emit(ProvidersEvent.ShowMessage(R.string.providers_added, cleanName))
            onDone(true)
        }
    }

    fun rename(
        provider: NullProvider,
        name: String,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.updateName(provider.id, cleanName)
            _events.emit(ProvidersEvent.ShowMessage(R.string.providers_renamed, cleanName))
        }
    }

    fun replaceToken(
        provider: NullProvider,
        token: String,
        onDone: (Boolean) -> Unit,
    ) {
        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) return
        viewModelScope.launch {
            if (!isTokenValid(ProviderKind.fromDbType(provider.type), cleanToken)) {
                onDone(false)
                return@launch
            }
            repository.updateToken(provider.id, cleanToken)
            if (provider.active) {
                refreshActive()
            }
            _events.emit(ProvidersEvent.ShowMessage(R.string.providers_token_replaced))
            onDone(true)
        }
    }

    /**
     * Renames an account and, when [token] is not blank, replaces its token after checking it.
     * @param onDone called with true when the changes were saved
     */
    fun edit(
        provider: NullProvider,
        name: String,
        token: String?,
        onDone: (Boolean) -> Unit,
    ) {
        val cleanName = name.trim()
        val cleanToken = token?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            if (cleanToken != null && !isTokenValid(ProviderKind.fromDbType(provider.type), cleanToken)) {
                onDone(false)
                return@launch
            }
            if (cleanName != provider.name) {
                repository.updateName(provider.id, cleanName)
            }
            if (cleanToken != null) {
                repository.updateToken(provider.id, cleanToken)
                if (provider.active) {
                    refreshActive()
                }
                _events.emit(ProvidersEvent.ShowMessage(R.string.providers_token_replaced))
            } else {
                _events.emit(ProvidersEvent.ShowMessage(R.string.providers_renamed, cleanName))
            }
            onDone(true)
        }
    }

    fun setDefault(provider: NullProvider) {
        viewModelScope.launch {
            repository.setActive(provider.id)
            refreshActive()
            _events.emit(ProvidersEvent.ShowMessage(R.string.providers_default_set, provider.name))
        }
    }

    fun delete(provider: NullProvider) {
        viewModelScope.launch {
            repository.deleteAndPromote(provider)
            if (provider.active) {
                refreshActive()
            }
            _events.emit(ProvidersEvent.ShowMessage(R.string.providers_deleted, provider.name))
        }
    }

    // keeps the shared API clients in sync with the default provider
    private suspend fun refreshActive() {
        debridProvider.activate()
        _events.emit(ProvidersEvent.ActiveChanged)
    }
}
