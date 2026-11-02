package com.github.livingwithhippos.nullseek

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridDownload
import com.github.livingwithhippos.nullseek.core.domain.DebridError
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import com.github.livingwithhippos.nullseek.core.domain.UserProfile
import com.github.livingwithhippos.nullseek.core.domain.describe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.delay
import com.github.livingwithhippos.nullseek.core.utils.NetworkMonitor
import com.github.livingwithhippos.nullseek.core.domain.DebridService
import org.koin.core.annotation.KoinViewModel

// waits before trying the user call again when the network is unreachable
private val NETWORK_RETRY_DELAYS_MS = listOf(1_000L, 2_000L)

@KoinViewModel
class NullRootViewModel(
    private val debridProvider: DebridServiceProvider,
    val providerRepository: NullProviderRepository,
    preferencesRepository: PreferencesRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NullRootUiState(loading = true))
    val uiState: StateFlow<NullRootUiState> = _uiState

    val themeMode: StateFlow<ThemeMode> =
        preferencesRepository.themeMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM,
        )

    val themeColor: StateFlow<ThemeColor> =
        preferencesRepository.themeColor.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeColor.SYSTEM,
        )

    val useStandardFont: StateFlow<Boolean> =
        preferencesRepository.useStandardFont.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    /** what the active provider supports, null until an account is loaded */
    val capabilities: StateFlow<DebridCapabilities?> =
        debridProvider.active
            .map { it?.capabilities }
            .stateIn(viewModelScope, SharingStarted.Eagerly, debridProvider.active.value?.capabilities)

    init {
        // check if there's an active token and enable it
        loadActiveToken()
        // the network often comes back a moment after the app starts or resumes: load again then
        networkMonitor.isOnline
            .filter { online -> online && _uiState.value.networkError && !_uiState.value.loading }
            .onEach { loadActiveToken() }
            .launchIn(viewModelScope)
    }

    /** Asks for the user, trying again a couple of times while the network is unreachable. */
    private suspend fun userWithRetry(service: DebridService): DebridResult<UserProfile> {
        var result = service.user()
        for (wait in NETWORK_RETRY_DELAYS_MS) {
            if (result !is DebridResult.Err || result.error !is DebridError.Network) break
            delay(wait)
            result = service.user()
        }
        return result
    }

    fun loadActiveToken() {
        viewModelScope.launch {
            val previousId = debridProvider.active.value?.provider?.id
            val active = debridProvider.activate()
            if (active == null) {
                setupNeeded()
                return@launch
            }
            if (previousId != active.provider.id) {
                // the lists and details belong to another account
                closeDetails()
            }
            _uiState.value = _uiState.value.copy(loading = true, provider = active.kind)
            // refresh user data on app start to cache it
            when (val response = userWithRetry(active.service)) {
                is DebridResult.Ok -> {
                    activeTokenLoaded(response.value)
                }

                is DebridResult.Err -> {
                    when (val error = response.error) {
                        DebridError.InvalidToken -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    loading = false,
                                    setup = false,
                                    user = null,
                                    tokenInvalid = true,
                                    networkError = false,
                                    error = "The ${active.kind.displayName} token for ${active.provider.name} is invalid or expired, replace it from Settings > Accounts",
                                )
                        }

                        is DebridError.Network -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    loading = false,
                                    setup = false,
                                    isOnline = false,
                                    networkError = true,
                                    error = "Unable to reach ${active.kind.displayName}, check your connection",
                                )
                        }

                        else -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    loading = false,
                                    setup = false,
                                    networkError = false,
                                    error = error.describe("Loading the ${active.kind.displayName} account"),
                                )
                        }
                    }
                }
            }
        }
    }

    fun checkValidToken(
        provider: NetProvider,
        token: String,
    ) {
        viewModelScope.launch {
            setLoading(true)
            val tempService = debridProvider.newTemp(provider.kind, token.trim())
            when (val response = tempService.user()) {
                is DebridResult.Ok -> {
                    providerRepository.insertActive(
                        NullProvider(
                            type = provider.type,
                            name = provider.name,
                            token = token.trim(),
                            active = true,
                        ),
                    )
                    val active = debridProvider.activate()
                    _uiState.value = _uiState.value.copy(provider = active?.kind)
                    setupCompleted(response.value)
                }

                is DebridResult.Err -> {
                    setupInvalid(
                        if (response.error is DebridError.Network) "Network error, try again" else "Invalid token",
                    )
                }
            }
        }
    }

    fun loadTorrentDetails(
        torrent: DebridTorrent,
        reload: Boolean = false,
    ) {
        viewModelScope.launch {
            if (reload) {
                loadingTorrentDetails()
                val response = debridProvider.current.torrent(torrent.id)
                if (response is DebridResult.Ok) {
                    loadedTorrentDetails(response.value)
                } else {
                    setupInvalid("Failed to load torrent details")
                }
            } else {
                loadedTorrentDetails(torrent)
            }
        }
    }

    fun loadWebDetails(
        webDownload: DebridDownload,
        reload: Boolean = false,
    ) {
        viewModelScope.launch {
            if (reload) {
                loadingWebDetails()
                val response = debridProvider.current.web(webDownload.id)
                if (response is DebridResult.Ok) {
                    loadedWebDetails(response.value)
                } else {
                    setupInvalid("Failed to load web details")
                }
            } else {
                loadedWebDetails(webDownload)
            }
        }
    }

    fun loadUsenetDetails(
        usenetDownload: DebridDownload,
        reload: Boolean = false,
    ) {
        viewModelScope.launch {
            if (reload) {
                loadingUsenetDetails()
                val response = debridProvider.current.usenet(usenetDownload.id)
                if (response is DebridResult.Ok) {
                    loadedUsenetDetails(response.value)
                } else {
                    setupInvalid("Failed to load usenet details")
                }
            } else {
                loadedUsenetDetails(usenetDownload)
            }
        }
    }

    fun closeDetails() {
        _uiState.value =
            _uiState.value.copy(
                selectedTorrent = null,
                selectedWeb = null,
                selectedUsenet = null,
                isDetailOnlyOpen = false,
            )
    }

    // helpers to update the state //

    // todo add a prefix for these

    private fun loadingTorrentDetails() {
        _uiState.value = _uiState.value.copy(loading = true, selectedTorrent = null, error = null)
    }

    private fun loadedTorrentDetails(torrent: DebridTorrent) {
        _uiState.value =
            _uiState.value.copy(loading = false, selectedTorrent = torrent, error = null)
    }

    private fun loadingWebDetails() {
        _uiState.value = _uiState.value.copy(loading = true, selectedWeb = null, error = null)
    }

    private fun loadedWebDetails(webDownload: DebridDownload) {
        _uiState.value =
            _uiState.value.copy(loading = false, selectedWeb = webDownload, error = null)
    }

    private fun loadingUsenetDetails() {
        _uiState.value = _uiState.value.copy(loading = true, selectedUsenet = null, error = null)
    }

    private fun loadedUsenetDetails(usenetDownload: DebridDownload) {
        _uiState.value =
            _uiState.value.copy(loading = false, selectedUsenet = usenetDownload, error = null)
    }

    private fun activeTokenLoaded(user: UserProfile) {
        _uiState.value =
            _uiState.value.copy(
                loading = false,
                setup = false,
                user = user,
                error = null,
                tokenInvalid = false,
                isOnline = true,
                networkError = false,
            )
    }

    private fun setupNeeded() {
        _uiState.value =
            NullRootUiState(loading = false, setup = true)
    }

    private fun setupCompleted(user: UserProfile) {
        _uiState.value =
            _uiState.value.copy(loading = false, setup = false, user = user, error = null, tokenInvalid = false)
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
     * user to cache the current user data
     */
    val user: UserProfile? = null,
    /**
     * currently selected a torrent, to show its details page
     */
    val selectedTorrent: DebridTorrent? = null,
    /**
     * currently selected a web download, to show its details page
     */
    val selectedWeb: DebridDownload? = null,
    val selectedUsenet: DebridDownload? = null,
    val isDetailOnlyOpen: Boolean = false,
    val error: String? = null,
    val isOnline: Boolean = true,
    /** the provider of the active account */
    val provider: ProviderKind? = null,
    /** the active account token was rejected */
    val tokenInvalid: Boolean = false,
    /** the last account load failed because the provider could not be reached */
    val networkError: Boolean = false,
)
