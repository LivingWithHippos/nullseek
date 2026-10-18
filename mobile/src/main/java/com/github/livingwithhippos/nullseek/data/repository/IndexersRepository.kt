package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.nullseek.core.data.repository.IndexerRepository
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.remote.IndexerRemoteClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IndexersRepository(
    private val coreRepository: IndexerRepository,
    private val indexerClient: IndexerRemoteClient,
) {
    val indexers: Flow<List<Indexer>> =
        coreRepository.getAllFlow().map { entities -> entities.map { Indexer.fromEntity(it) } }

    suspend fun getAll(): List<Indexer> = coreRepository.getAll().map { Indexer.fromEntity(it) }

    suspend fun addIndexer(indexer: Indexer): Long = coreRepository.insert(indexer.toEntity())

    suspend fun updateIndexer(indexer: Indexer) = coreRepository.update(indexer.toEntity())

    suspend fun deleteIndexer(indexer: Indexer) = coreRepository.delete(indexer.toEntity())

    suspend fun setActive(
        id: Long,
        active: Boolean,
    ) = coreRepository.setActive(id, active)

    suspend fun testConnection(indexer: Indexer): Result<Unit> =
        indexerClient.testConnection(indexer.type, indexer.address, indexer.apiToken)
}
