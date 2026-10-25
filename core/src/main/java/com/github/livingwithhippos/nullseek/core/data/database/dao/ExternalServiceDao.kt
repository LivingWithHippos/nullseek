package com.github.livingwithhippos.nullseek.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExternalServiceDao {
    @Query("SELECT * FROM external_services ORDER BY id ASC")
    fun getAllFlow(): Flow<List<ExternalServiceEntity>>

    @Query("SELECT * FROM external_services")
    suspend fun getAll(): List<ExternalServiceEntity>

    @Query("SELECT * FROM external_services WHERE category = :category ORDER BY id ASC")
    fun getByCategoryFlow(category: String): Flow<List<ExternalServiceEntity>>

    @Query("SELECT * FROM external_services WHERE category = :category ORDER BY id ASC")
    suspend fun getByCategory(category: String): List<ExternalServiceEntity>

    @Query("SELECT * FROM external_services WHERE category = :category AND is_default = 1 LIMIT 1")
    fun getDefaultByCategoryFlow(category: String): Flow<ExternalServiceEntity?>

    @Query("SELECT * FROM external_services WHERE category = :category AND is_default = 1 LIMIT 1")
    suspend fun getDefaultByCategory(category: String): ExternalServiceEntity?

    @Query("SELECT * FROM external_services WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExternalServiceEntity?

    @Query("UPDATE external_services SET is_default = CASE WHEN id = :id THEN 1 ELSE 0 END WHERE category = :category")
    suspend fun setDefault(
        id: Long,
        category: String,
    )

    @Query("UPDATE external_services SET is_default = 0 WHERE category = :category")
    suspend fun clearDefault(category: String)

    @Insert
    suspend fun insert(service: ExternalServiceEntity): Long

    @Update
    suspend fun update(service: ExternalServiceEntity)

    @Delete
    suspend fun delete(service: ExternalServiceEntity)

    @Query("DELETE FROM external_services WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM external_services")
    suspend fun deleteAll()
}
