package com.github.livingwithhippos.nullseek.core.data.repository

import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import kotlinx.coroutines.flow.Flow

interface NullProviderRepository {
    suspend fun getActive(): NullProvider?

    suspend fun getById(id: Long): NullProvider?

    suspend fun getAll(): List<NullProvider>

    suspend fun setActive(id: Long)

    suspend fun insert(provider: NullProvider): Long

    suspend fun upsert(provider: NullProvider): Long

    suspend fun delete(provider: NullProvider)

    suspend fun deleteAll()

    suspend fun insertActive(provider: NullProvider): Long

    suspend fun updateName(
        id: Long,
        name: String,
    )

    suspend fun updateToken(
        id: Long,
        token: String,
    )

    fun observeAll(): Flow<List<NullProvider>>

    /** Deletes the provider; if it was the active one, promotes another provider (if any). */
    suspend fun deleteAndPromote(provider: NullProvider)
}

class NullProviderRepositoryImpl(
    private val nullProviderDao: NullProviderDao,
) : NullProviderRepository {
    override suspend fun getActive(): NullProvider? = nullProviderDao.getActive()

    override suspend fun getById(id: Long): NullProvider? = nullProviderDao.getById(id)

    override suspend fun getAll(): List<NullProvider> = nullProviderDao.getAll()

    override suspend fun setActive(id: Long) = nullProviderDao.setActive(id)

    override suspend fun insert(provider: NullProvider): Long = nullProviderDao.insert(provider)

    override suspend fun upsert(provider: NullProvider): Long = nullProviderDao.upsert(provider)

    override suspend fun delete(provider: NullProvider) = nullProviderDao.delete(provider)

    override suspend fun deleteAll() = nullProviderDao.deleteAll()

    override suspend fun insertActive(provider: NullProvider): Long {
        val id = nullProviderDao.insert(provider)
        // we need to call this again to be sure the others have their active flag set to false
        nullProviderDao.setActive(id)
        return id
    }

    override suspend fun updateName(
        id: Long,
        name: String,
    ) = nullProviderDao.updateName(id, name)

    override suspend fun updateToken(
        id: Long,
        token: String,
    ) = nullProviderDao.updateToken(id, token)

    override fun observeAll(): Flow<List<NullProvider>> = nullProviderDao.observeAll()

    override suspend fun deleteAndPromote(provider: NullProvider) {
        nullProviderDao.delete(provider)
        if (nullProviderDao.getActive() == null) {
            nullProviderDao.getAll().firstOrNull()?.let { nullProviderDao.setActive(it.id) }
        }
    }
}
