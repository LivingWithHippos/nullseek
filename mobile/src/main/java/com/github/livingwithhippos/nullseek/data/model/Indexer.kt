package com.github.livingwithhippos.nullseek.data.model

import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity

enum class IndexerType(
    val defaultPort: Int,
    val displayName: String,
) {
    JACKETT(9117, "Jackett"),
    PROWLARR(9696, "Prowlarr"),
    ;

    companion object {
        fun fromString(value: String): IndexerType = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: JACKETT
    }
}

data class Indexer(
    val id: Long = 0,
    val name: String,
    val type: IndexerType,
    val address: String,
    val apiToken: String = "",
    val active: Boolean = true,
) {
    fun toEntity(): IndexerEntity =
        IndexerEntity(
            id = id,
            name = name,
            type = type.name,
            address = address,
            apiToken = apiToken,
            active = active,
        )

    companion object {
        fun fromEntity(entity: IndexerEntity): Indexer =
            Indexer(
                id = entity.id,
                name = entity.name,
                type = IndexerType.fromString(entity.type),
                address = entity.address,
                apiToken = entity.apiToken,
                active = entity.active,
            )
    }
}
