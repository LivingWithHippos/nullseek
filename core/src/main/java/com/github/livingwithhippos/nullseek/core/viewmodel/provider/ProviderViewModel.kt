package com.github.livingwithhippos.nullseek.core.viewmodel.provider

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ProviderViewModel(
    private val dataStore: DataStore<Preferences>,
    private val client: TorboxClient,
    val providerRepository: NullProviderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderScreenUiState())

    val state: StateFlow<ProviderScreenUiState>
        get() = _state

    private val _events = MutableSharedFlow<ProviderEvent>()
    val events = _events.asSharedFlow()

    fun onProviderAction(action: ProviderAction) {
        when (action) {
            is ProviderAction.UserConfirmed -> {
                saveTokenAndShowUser(action.provider, action.token)
            }

            is ProviderAction.CheckToken -> {
                checkTorboxToken(action.token)
            }
        }
    }

    fun checkTorboxToken(token: String) {
        viewModelScope.launch {
            _state.value = ProviderScreenUiState(
                isLoading = true, errorMessage = null, user = null, validToken = null
            )

            // todo: restore token on failure?
            client.updateBearerToken(token)
            val result = client.user.data()

            if (result is ApiResult.Success && result.value.data != null) {
                // show user recap and confirm button
                _state.value = ProviderScreenUiState(
                    isLoading = false,
                    errorMessage = null,
                    user = result.value.data,
                    validToken = token
                )
            } else {
                _state.value = ProviderScreenUiState(
                    isLoading = false,
                    errorMessage = "Invalid Token",
                    user = null,
                    validToken = null
                )

            }
        }

    }

    fun saveTokenAndShowUser(provider: NetProvider, token: String) {
        client.updateBearerToken(token)
        viewModelScope.launch {
            val id = providerRepository.insert(
                NullProvider(
                    type= provider.type,
                    name=provider.name,
                    token=token,
                )
            )
            providerRepository.setActive(id)
            _events.emit(
                ProviderEvent.TokenConfirmed
            )
        }
    }
}
@Immutable
sealed interface ProviderAction {
    data class UserConfirmed(val provider: NetProvider, val token: String) : ProviderAction
    data class CheckToken(val token: String) : ProviderAction
}

@Immutable
data class ProviderScreenUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: UserDataDto? = null,
    val validToken: String? = null
)

sealed interface ProviderEvent {
    object TokenConfirmed : ProviderEvent
}