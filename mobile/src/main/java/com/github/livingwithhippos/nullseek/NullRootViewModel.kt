package com.github.livingwithhippos.nullseek

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.network.ApiResult
import com.github.livingwithhippos.torbox.network.TorboxClientConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NullRootViewModel(
    private val client: TorboxClient,
    val providerRepository: NullProviderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NullRootUiState(loading = true))
    val uiState: StateFlow<NullRootUiState> = _uiState

    // todo: decide if we should have a factory method for temp clients
    private val tempClient =
        TorboxClient.create(
            TorboxClientConfig(),
        )

    init {
        // check if there's an active token and enable it, initializing the TorboxClient with the token
        loadActiveToken()
    }

    fun loadActiveToken() {
        viewModelScope.launch {
            val activeProvider = providerRepository.getActive()
            if (activeProvider != null) {
                client.updateBearerToken(activeProvider.token)
                // refresh user data on app start to cache it
                val response = client.user.data()
                if (response is ApiResult.Success) {
                    val user = response.value.data
                    if (user != null) {
                        activeTokenLoaded(user)
                    } else {
                        // todo: manage this (should not happen)
                    }
                } else {
                    when (response) {
                        is ApiResult.Failure.BadRequest -> TODO()
                        is ApiResult.Failure.Http -> TODO()
                        is ApiResult.Failure.Network -> TODO()
                        is ApiResult.Failure.NoSuccess<*> -> TODO()
                        is ApiResult.Failure.Serialization -> TODO()
                    }
                }
            } else {
                setupNeeded()
            }
        }
    }

    fun checkValidToken(
        provider: NetProvider,
        token: String,
    ) {
        viewModelScope.launch {
            setLoading(true)
            tempClient.updateBearerToken(token)
            val response = tempClient.user.data()

            if (response is ApiResult.Success) {
                val user = response.value.data
                if (user != null) {
                    // this should update the singleton value
                    client.updateBearerToken(token)
                    providerRepository.insertActive(
                        NullProvider(
                            type = provider.type,
                            name = provider.name,
                            token = token,
                            active = true,
                        ),
                    )
                    setupCompleted(user)
                } else {
                    setupInvalid("Invalid token")
                }
            } else {
                setupInvalid("Invalid token")
            }
        }
    }

    fun loadTorrentDetails(
        torrent: TorrentDto,
        reload: Boolean = false,
    ) {
        viewModelScope.launch {
            if (reload) {
                loadingTorrentDetails()
                val response = client.torrent.get(id = torrent.id)
                if (response is ApiResult.Success) {
                    val torrentDetails = response.value.data
                    if (torrentDetails != null) {
                        loadedTorrentDetails(torrentDetails)
                    } else {
                        setupInvalid("Failed to load torrent details")
                    }
                } else {
                    setupInvalid("Failed to load torrent details")
                }
            } else {
                loadedTorrentDetails(torrent)
            }
        }
    }

    // helpers to update the state //

    // todo add a prefix for these

    fun showSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(settings = show)
    }

    private fun loadingTorrentDetails() {
        _uiState.value = _uiState.value.copy(loading = true, selectedTorrent = null, error = null)
    }

    private fun loadedTorrentDetails(torrent: TorrentDto) {
        _uiState.value = _uiState.value.copy(loading = false, selectedTorrent = torrent, error = null)
    }

    private fun activeTokenLoaded(user: UserDataDto) {
        _uiState.value = _uiState.value.copy(loading = false, setup = false, user = user, error = null)
    }

    private fun setupNeeded() {
        _uiState.value = _uiState.value.copy(loading = false, setup = true, user = null)
    }

    private fun setupCompleted(user: UserDataDto) {
        _uiState.value = _uiState.value.copy(loading = false, setup = false, user = user, error = null)
    }

    private fun setupInvalid(error: String) {
        _uiState.value = _uiState.value.copy(loading = false, error = error)
    }

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(loading = loading)
    }
}

@Immutable
data class NullRootUiState(
    /**
     * used when starting the app and checking:
     * - token available
     * - network available
     * - token valid
     * then either we react to an issue or load the user
     */
    val loading: Boolean = false,
    /**
     * used when there's no token or the only token in invalid
     * todo: check error returned by the API when it's expired/invalid
     */
    val setup: Boolean = false,
    /**
     * used to show the settings page
     */
    val settings: Boolean = false,
    /**
     * user to cache the current user data
     */
    val user: UserDataDto? = null,
    /**
     * currently selected a torrent, to show its details page
     */
    val selectedTorrent: TorrentDto? = null,
    /**
     * currently selected a web download, to show its details page
     */
    val selectedWeb: Int? = null,
    /**
     *
     */
    val isDetailOnlyOpen: Boolean = false,
    val error: String? = null,
    val isOnline: Boolean = true,
)
