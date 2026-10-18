package com.github.livingwithhippos.nullseek.core.data.repository

import com.github.livingwithhippos.nullseek.core.data.database.dao.IndexerDao
import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity
import kotlinx.coroutines.flow.Flow

interface IndexerRepository {
    fun getAllFlow(): Flow<List<IndexerEntity>>

    suspend fun getAll(): List<IndexerEntity>

    suspend fun getById(id: Long): IndexerEntity?

    suspend fun insert(indexer: IndexerEntity): Long

    suspend fun update(indexer: IndexerEntity)

    suspend fun delete(indexer: IndexerEntity)

    suspend fun setActive(
        id: Long,
        active: Boolean,
    )
}

class IndexerRepositoryImpl(
    private val indexerDao: IndexerDao,
) : IndexerRepository {
    override fun getAllFlow(): Flow<List<IndexerEntity>> = indexerDao.getAllFlow()

    override suspend fun getAll(): List<IndexerEntity> = indexerDao.getAll()

    override suspend fun getById(id: Long): IndexerEntity? = indexerDao.getById(id)

    override suspend fun insert(indexer: IndexerEntity): Long = indexerDao.insert(indexer)

    override suspend fun update(indexer: IndexerEntity) = indexerDao.update(indexer)

    override suspend fun delete(indexer: IndexerEntity) = indexerDao.delete(indexer)

    override suspend fun setActive(
        id: Long,
        active: Boolean,
    ) = indexerDao.setActive(id, active)
}
