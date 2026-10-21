package com.github.livingwithhippos.nullseek.core.data.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Database entity for installed Stremio addons
 */
@Entity(tableName = "stremio_addons")
data class StremioAddonEntity(
    @PrimaryKey val manifestUrl: String,
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val resources: String, // JSON array serialized as string
    val types: String, // JSON array serialized as string
    val logo: String? = null,
    val background: String? = null,
    val isEnabled: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis(),
)
