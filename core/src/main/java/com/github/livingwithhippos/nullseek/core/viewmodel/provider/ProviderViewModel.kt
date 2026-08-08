package com.github.livingwithhippos.nullseek.core.viewmodel.provider

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.network.ApiResult
import com.github.livingwithhippos.torbox.network.RestClientConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProviderViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProviderScreenUiState())

    val state: StateFlow<ProviderScreenUiState>
        get() = _state

    fun checkTorboxToken(token: String) {
        viewModelScope.launch {
            _state.value = ProviderScreenUiState(
                isLoading = true, errorMessage = null, user = null, validToken = null
            )

            val client = TorboxClient.create(RestClientConfig(bearerToken = token, logLevel = "INFO"))
            val result = client.user.data()

            if (result is ApiResult.Success && result.value.data != null) {
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
}

@Immutable
data class ProviderScreenUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: UserDataDto? = null,
    val validToken: String? = null
)