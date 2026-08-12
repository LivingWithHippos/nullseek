package com.github.livingwithhippos.nullseek.core.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "providers",
)
data class NullProvider(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "token") val token: String,
    @ColumnInfo(name = "active") val active: Boolean = false,
)
