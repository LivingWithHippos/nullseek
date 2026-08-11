package com.github.livingwithhippos.nullseek.ui.user

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UserViewModel(
    private val torClient: TorboxClient
) : ViewModel() {

    private val _state = MutableStateFlow(UserScreenUiState(isLoading = true))

    val state: StateFlow<UserScreenUiState>
        get() = _state

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _state.value = UserScreenUiState(isLoading = true)

            val result = torClient.user.data()

            if (result is ApiResult.Success && result.value.data != null) {
                _state.value = UserScreenUiState(
                    isLoading = false,
                    user = result.value.data
                )
            } else {
                // todo: switch to when to handle different ApiResult types
                _state.value = UserScreenUiState(
                    isLoading = false,
                    user = null,
                    error = result.toString()
                )
            }
        }
    }
}

@Immutable
data class UserScreenUiState(
    val isLoading: Boolean = false,
    val user: UserDataDto? = null,
    val error: String? = null
)