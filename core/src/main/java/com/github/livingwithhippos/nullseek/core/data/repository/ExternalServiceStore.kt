package com.github.livingwithhippos.nullseek.core.data.repository

import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import kotlinx.coroutines.flow.Flow

interface ExternalServiceRepository {
    fun getAllFlow(): Flow<List<ExternalServiceEntity>>

    suspend fun getAll(): List<ExternalServiceEntity>

    fun getByCategoryFlow(category: String): Flow<List<ExternalServiceEntity>>

    suspend fun getByCategory(category: String): List<ExternalServiceEntity>

    fun getDefaultByCategoryFlow(category: String): Flow<ExternalServiceEntity?>

    suspend fun getDefaultByCategory(category: String): ExternalServiceEntity?

    suspend fun getById(id: Long): ExternalServiceEntity?

    suspend fun setDefault(
        id: Long,
        category: String,
    )

    suspend fun clearDefault(category: String)

    suspend fun insert(service: ExternalServiceEntity): Long

    suspend fun update(service: ExternalServiceEntity)

    suspend fun delete(service: ExternalServiceEntity)

    suspend fun deleteById(id: Long)
}

class ExternalServiceRepositoryImpl(
    private val externalServiceDao: ExternalServiceDao,
) : ExternalServiceRepository {
    override fun getAllFlow(): Flow<List<ExternalServiceEntity>> = externalServiceDao.getAllFlow()

    override suspend fun getAll(): List<ExternalServiceEntity> = externalServiceDao.getAll()

    override fun getByCategoryFlow(category: String): Flow<List<ExternalServiceEntity>> = externalServiceDao.getByCategoryFlow(category)

    override suspend fun getByCategory(category: String): List<ExternalServiceEntity> = externalServiceDao.getByCategory(category)

    override fun getDefaultByCategoryFlow(category: String): Flow<ExternalServiceEntity?> =
        externalServiceDao.getDefaultByCategoryFlow(category)

    override suspend fun getDefaultByCategory(category: String): ExternalServiceEntity? = externalServiceDao.getDefaultByCategory(category)

    override suspend fun getById(id: Long): ExternalServiceEntity? = externalServiceDao.getById(id)

    override suspend fun setDefault(
        id: Long,
        category: String,
    ) = externalServiceDao.setDefault(id, category)

    override suspend fun clearDefault(category: String) = externalServiceDao.clearDefault(category)

    override suspend fun insert(service: ExternalServiceEntity): Long = externalServiceDao.insert(service)

    override suspend fun update(service: ExternalServiceEntity) = externalServiceDao.update(service)

    override suspend fun delete(service: ExternalServiceEntity) = externalServiceDao.delete(service)

    override suspend fun deleteById(id: Long) = externalServiceDao.deleteById(id)
}
