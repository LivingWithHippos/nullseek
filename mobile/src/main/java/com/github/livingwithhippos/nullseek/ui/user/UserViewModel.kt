package com.github.livingwithhippos.nullseek.ui.user

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.UserRepository
import com.github.livingwithhippos.torbox.model.UserDataDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UserViewModel(
    val userRepository: UserRepository,
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

            val user = userRepository.getUserData()

            if (user != null) {
                _state.value =
                    UserScreenUiState(
                        isLoading = false,
                        user = user,
                    )
            } else {
                // todo: show errors and retry/refresh button
                _state.value =
                    UserScreenUiState(
                        isLoading = false,
                        user = null,
                        error = "Failed to load user data",
                    )
            }
        }
    }
}

@Immutable
data class UserScreenUiState(
    val isLoading: Boolean = false,
    val user: UserDataDto? = null,
    val error: String? = null,
)
