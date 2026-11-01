package com.github.livingwithhippos.nullseek.core.domain

import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAccount(
    val provider: NullProvider,
    val service: DebridService,
) {
    val kind: ProviderKind get() = service.kind
    val capabilities: DebridCapabilities get() = service.capabilities
}

/**
 * Picks the service for the active account in the providers table.
 * Call [activate] whenever the active account or its token changes.
 */
class DebridServiceProvider(
    private val services: Map<ProviderKind, DebridService>,
    private val repository: NullProviderRepository,
    private val tempFactory: (ProviderKind) -> DebridService,
) {
    private val _active = MutableStateFlow<ActiveAccount?>(null)
    val active: StateFlow<ActiveAccount?> = _active.asStateFlow()

    /** the service of the active account, TorBox when none is set up yet */
    val current: DebridService get() = _active.value?.service ?: forKind(ProviderKind.TORBOX)

    val capabilities: DebridCapabilities get() = current.capabilities

    fun forKind(kind: ProviderKind): DebridService = services.getValue(kind)

    /** a separate service instance, to check a token or to work in the background */
    fun newTemp(
        kind: ProviderKind,
        token: String? = null,
    ): DebridService = tempFactory(kind).also { service -> token?.let { service.setToken(it) } }

    suspend fun activate(): ActiveAccount? {
        val provider = repository.getActive()
        if (provider == null) {
            _active.value = null
            return null
        }
        val service = forKind(ProviderKind.fromDbType(provider.type))
        service.setToken(provider.token)
        return ActiveAccount(provider, service).also { _active.value = it }
    }
}
