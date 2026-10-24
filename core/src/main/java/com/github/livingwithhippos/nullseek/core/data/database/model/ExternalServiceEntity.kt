package com.github.livingwithhippos.nullseek.core.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "external_services",
)
data class ExternalServiceEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "username") val username: String = "",
    @ColumnInfo(name = "password") val password: String = "",
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
)
