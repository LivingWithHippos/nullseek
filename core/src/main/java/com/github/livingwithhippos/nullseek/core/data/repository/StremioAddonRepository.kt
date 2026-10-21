package com.github.livingwithhippos.nullseek.core.data.repository

import com.github.livingwithhippos.nullseek.core.data.database.dao.StremioAddonDao
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import kotlinx.coroutines.flow.Flow

interface StremioAddonRepository {
    fun getAllAddons(): Flow<List<StremioAddonEntity>>

    fun getEnabledAddons(): Flow<List<StremioAddonEntity>>

    suspend fun getAddonByUrl(manifestUrl: String): StremioAddonEntity?

    suspend fun insertAddon(addon: StremioAddonEntity)

    suspend fun updateAddon(addon: StremioAddonEntity)

    suspend fun setAddonEnabled(
        manifestUrl: String,
        enabled: Boolean,
    )

    suspend fun deleteAddon(manifestUrl: String)

    suspend fun deleteAllAddons()

    suspend fun getAddonCount(): Int
}

class StremioAddonRepositoryImpl(
    private val dao: StremioAddonDao,
) : StremioAddonRepository {
    override fun getAllAddons(): Flow<List<StremioAddonEntity>> = dao.getAllAddons()

    override fun getEnabledAddons(): Flow<List<StremioAddonEntity>> = dao.getEnabledAddons()

    override suspend fun getAddonByUrl(manifestUrl: String): StremioAddonEntity? = dao.getAddonByUrl(manifestUrl)

    override suspend fun insertAddon(addon: StremioAddonEntity) = dao.insertAddon(addon)

    override suspend fun updateAddon(addon: StremioAddonEntity) = dao.updateAddon(addon)

    override suspend fun setAddonEnabled(
        manifestUrl: String,
        enabled: Boolean,
    ) = dao.setAddonEnabled(manifestUrl, enabled)

    override suspend fun deleteAddon(manifestUrl: String) = dao.deleteAddon(manifestUrl)

    override suspend fun deleteAllAddons() = dao.deleteAllAddons()

    override suspend fun getAddonCount(): Int = dao.getAddonCount()
}
