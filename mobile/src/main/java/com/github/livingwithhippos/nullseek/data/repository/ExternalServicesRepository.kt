package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.nullseek.core.data.repository.ExternalServiceRepository
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceType
import com.github.livingwithhippos.nullseek.data.remote.KodiRemoteClient
import com.github.livingwithhippos.nullseek.data.remote.VlcRemoteClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExternalServicesRepository(
    private val coreRepository: ExternalServiceRepository,
    private val kodiClient: KodiRemoteClient,
    private val vlcClient: VlcRemoteClient,
) {
    val allServices: Flow<List<ExternalService>> =
        coreRepository.getAllFlow().map { entities ->
            entities.map { ExternalService.fromEntity(it) }
        }

    val mediaPlayerServices: Flow<List<ExternalService>> =
        coreRepository.getByCategoryFlow(ExternalServiceCategory.MEDIA_PLAYER.name)
            .map { entities ->
                entities.map { ExternalService.fromEntity(it) }
            }

    val defaultMediaPlayer: Flow<ExternalService?> =
        coreRepository.getDefaultByCategoryFlow(ExternalServiceCategory.MEDIA_PLAYER.name)
            .map { entity ->
                entity?.let { ExternalService.fromEntity(it) }
            }

    suspend fun getAll(): List<ExternalService> =
        coreRepository.getAll().map { ExternalService.fromEntity(it) }

    suspend fun getMediaPlayers(): List<ExternalService> =
        coreRepository.getByCategory(ExternalServiceCategory.MEDIA_PLAYER.name)
            .map { ExternalService.fromEntity(it) }

    suspend fun getDefaultMediaPlayer(): ExternalService? =
        coreRepository.getDefaultByCategory(ExternalServiceCategory.MEDIA_PLAYER.name)?.let {
            ExternalService.fromEntity(it)
        }

    suspend fun addService(service: ExternalService): Long {
        val id = coreRepository.insert(service.toEntity())
        if (service.isDefault) {
            coreRepository.setDefault(id, service.type.category.name)
        }
        return id
    }

    suspend fun updateService(service: ExternalService) {
        coreRepository.update(service.toEntity())
        if (service.isDefault) {
            coreRepository.setDefault(service.id, service.type.category.name)
        }
    }

    suspend fun deleteService(id: Long) {
        coreRepository.deleteById(id)
    }

    suspend fun setDefault(
        id: Long,
        category: ExternalServiceCategory,
    ) {
        coreRepository.setDefault(id, category.name)
    }

    suspend fun clearDefault(category: ExternalServiceCategory) {
        coreRepository.clearDefault(category.name)
    }

    suspend fun testConnection(service: ExternalService): Result<Unit> =
        when (service.type) {
            ExternalServiceType.KODI -> {
                kodiClient.testConnection(
                    address = service.address,
                    username = service.username.ifBlank { null },
                    password = service.password.ifBlank { null },
                )
            }

            ExternalServiceType.VLC -> {
                vlcClient.testConnection(
                    address = service.address,
                    username = service.username.ifBlank { null },
                    password = service.password.ifBlank { null },
                )
            }

            ExternalServiceType.JACKETT, ExternalServiceType.PROWLARR -> {
                Result.failure(UnsupportedOperationException("Testing for ${service.type.displayName} is not yet supported."))
            }
        }

    suspend fun playMedia(
        service: ExternalService,
        mediaUrl: String,
    ): Result<Unit> =
        when (service.type) {
            ExternalServiceType.KODI -> {
                kodiClient.openUrl(
                    address = service.address,
                    mediaUrl = mediaUrl,
                    username = service.username.ifBlank { null },
                    password = service.password.ifBlank { null },
                )
            }

            ExternalServiceType.VLC -> {
                vlcClient.openUrl(
                    address = service.address,
                    mediaUrl = mediaUrl,
                    username = service.username.ifBlank { null },
                    password = service.password.ifBlank { null },
                )
            }

            ExternalServiceType.JACKETT, ExternalServiceType.PROWLARR -> {
                Result.failure(UnsupportedOperationException("${service.type.displayName} is not a media player service."))
            }
        }
}
