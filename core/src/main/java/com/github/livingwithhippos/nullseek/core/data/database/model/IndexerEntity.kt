package com.github.livingwithhippos.nullseek.core.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "indexers",
)
data class IndexerEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "api_token") val apiToken: String = "",
    @ColumnInfo(name = "active") val active: Boolean = true,
)
