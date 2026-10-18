package com.github.livingwithhippos.nullseek.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IndexerDao {
    @Query("SELECT * FROM indexers ORDER BY id ASC")
    fun getAllFlow(): Flow<List<IndexerEntity>>

    @Query("SELECT * FROM indexers ORDER BY id ASC")
    suspend fun getAll(): List<IndexerEntity>

    @Query("SELECT * FROM indexers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): IndexerEntity?

    @Insert
    suspend fun insert(indexer: IndexerEntity): Long

    @Update
    suspend fun update(indexer: IndexerEntity)

    @Delete
    suspend fun delete(indexer: IndexerEntity)

    @Query("UPDATE indexers SET active = :active WHERE id = :id")
    suspend fun setActive(
        id: Long,
        active: Boolean,
    )
}
