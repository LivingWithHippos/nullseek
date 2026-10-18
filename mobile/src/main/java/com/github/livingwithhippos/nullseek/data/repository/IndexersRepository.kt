package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.nullseek.core.data.repository.IndexerRepository
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.remote.IndexerRemoteClient
import com.github.livingwithhippos.nullseek.data.model.IndexerSearchOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
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

    /**
     * Queries all [indexers] concurrently, emitting one outcome per indexer as soon as it completes.
     * A failing indexer produces a [IndexerSearchOutcome.Failure] without affecting the others.
     */
    fun search(
        indexers: List<Indexer>,
        query: String,
    ): Flow<IndexerSearchOutcome> =
        channelFlow {
            indexers.forEach { indexer ->
                launch {
                    val outcome =
                        indexerClient
                            .search(indexer.type, indexer.address, indexer.apiToken, query, indexer.name)
                            .fold(
                                onSuccess = { IndexerSearchOutcome.Success(indexer, it) },
                                onFailure = { IndexerSearchOutcome.Failure(indexer, it) },
                            )
                    send(outcome)
                }
            }
        }
}
