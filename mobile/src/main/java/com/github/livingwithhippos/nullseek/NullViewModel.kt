package com.github.livingwithhippos.nullseek

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.torbox.client.TorboxClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NullViewModel(
    private val client: TorboxClient,
    val providerRepository: NullProviderRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        // check if there's an active token and enable it, initializing the TorboxClient with the token
        loadActiveToken()
    }

    fun onMissingToken() {
        // Emit an event to show a snackbar with a message and an action label
        viewModelScope.launch {
            _events.emit(
                UiEvent.ShowSnackbar(
                    message = "Token is required",
                    actionLabel = "Dismiss",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
            )
        }
    }

    fun loadActiveToken() {
        viewModelScope.launch {
            val activeProvider = providerRepository.getActive()
            if (activeProvider != null) {
                client.updateBearerToken(activeProvider.token)
                _events.emit(UiEvent.NavigateToUser)
            } else {
                // todo: change UI so it has a splash screen first, then we can go to the user or setup screen
                _events.emit(UiEvent.NavigateToSetup)
            }
        }
    }
}

sealed interface UiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : UiEvent
    data object NavigateToSetup : UiEvent
    data object NavigateToUser : UiEvent
}