package com.github.livingwithhippos.nullseek.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StremioAddonDao {
    @Query("SELECT * FROM stremio_addons ORDER BY addedTimestamp DESC")
    fun getAllAddons(): Flow<List<StremioAddonEntity>>

    @Query("SELECT * FROM stremio_addons WHERE isEnabled = 1 ORDER BY addedTimestamp DESC")
    fun getEnabledAddons(): Flow<List<StremioAddonEntity>>

    @Query("SELECT * FROM stremio_addons WHERE manifestUrl = :manifestUrl")
    suspend fun getAddonByUrl(manifestUrl: String): StremioAddonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddon(addon: StremioAddonEntity)

    @Update
    suspend fun updateAddon(addon: StremioAddonEntity)

    @Query("UPDATE stremio_addons SET isEnabled = :enabled WHERE manifestUrl = :manifestUrl")
    suspend fun setAddonEnabled(
        manifestUrl: String,
        enabled: Boolean,
    )

    @Query("UPDATE stremio_addons SET customName = :customName, isEnabled = :enabled WHERE manifestUrl = :manifestUrl")
    suspend fun updateAddonDetails(
        manifestUrl: String,
        customName: String?,
        enabled: Boolean,
    )

    @Query("DELETE FROM stremio_addons WHERE manifestUrl = :manifestUrl")
    suspend fun deleteAddon(manifestUrl: String)

    @Query("DELETE FROM stremio_addons")
    suspend fun deleteAllAddons()

    @Query("SELECT COUNT(*) FROM stremio_addons")
    suspend fun getAddonCount(): Int
}
