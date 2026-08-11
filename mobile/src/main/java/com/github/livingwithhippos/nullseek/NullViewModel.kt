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
class NullViewModel : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

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
}

sealed interface UiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : UiEvent
}