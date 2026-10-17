package com.github.livingwithhippos.nullseek.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface NullProviderDao {
    @Query("SELECT * FROM providers WHERE active = 1 LIMIT 1")
    suspend fun getActive(): NullProvider?

    @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NullProvider?

    @Query("SELECT * FROM providers")
    suspend fun getAll(): List<NullProvider>

    @Query("UPDATE providers SET active = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActive(id: Long)

    @Query("UPDATE providers SET name = :name WHERE id = :id")
    suspend fun updateName(
        id: Long,
        name: String,
    )

    @Query("UPDATE providers SET token = :token WHERE id = :id")
    suspend fun updateToken(
        id: Long,
        token: String,
    )

    @Query("SELECT * FROM providers ORDER BY id")
    fun observeAll(): Flow<List<NullProvider>>

    @Insert
    suspend fun insert(provider: NullProvider): Long

    @Upsert
    suspend fun upsert(provider: NullProvider): Long

    @Delete
    suspend fun delete(provider: NullProvider)

    @Query("DELETE FROM providers")
    suspend fun deleteAll()
}
