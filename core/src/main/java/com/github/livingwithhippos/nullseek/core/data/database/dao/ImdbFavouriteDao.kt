package com.github.livingwithhippos.nullseek.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.github.livingwithhippos.nullseek.core.data.database.model.ImdbFavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImdbFavouriteDao {
    @Query("SELECT * FROM imdb_favourites ORDER BY addedTimestamp DESC")
    fun observeAll(): Flow<List<ImdbFavouriteEntity>>

    @Query("SELECT * FROM imdb_favourites ORDER BY addedTimestamp DESC")
    suspend fun getAll(): List<ImdbFavouriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: ImdbFavouriteEntity)

    @Query("DELETE FROM imdb_favourites WHERE imdbId = :imdbId")
    suspend fun delete(imdbId: String)

    @Query("DELETE FROM imdb_favourites")
    suspend fun deleteAll()
}
