package com.github.livingwithhippos.nullseek.core.data.repository

import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider

interface NullProviderRepository {
    suspend fun getActive(): NullProvider?

    suspend fun getById(id: Long): NullProvider?

    suspend fun getAll(): List<NullProvider>

    suspend fun setActive(id: Long)

    suspend fun insert(provider: NullProvider): Long

    suspend fun upsert(provider: NullProvider): Long

    suspend fun delete(provider: NullProvider)

    suspend fun deleteAll()
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
}
