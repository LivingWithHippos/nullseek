package com.github.livingwithhippos.nullseek.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.github.livingwithhippos.nullseek.core.data.database.model.TorboxUserCache

@Dao
interface TorboxUserCacheDao {

    @Upsert
    suspend fun upsert(user: TorboxUserCache)

    @Query("SELECT * FROM torbox_user_cache LIMIT 1")
    suspend fun get(): TorboxUserCache?

    @Query("SELECT * FROM torbox_user_cache")
    suspend fun getAll(): List<TorboxUserCache>

    @Query("SELECT * FROM torbox_user_cache WHERE provider_id = :id LIMIT 1")
    suspend fun getById(id: Long): TorboxUserCache?


    @Query("DELETE FROM torbox_user_cache")
    suspend fun clear()
}