package com.github.livingwithhippos.nullseek.data.model

import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity

enum class ExternalServiceCategory(
    val displayName: String,
) {
    MEDIA_PLAYER("Media Player"),
    INDEXER("Indexer"),
}

enum class ExternalServiceType(
    val category: ExternalServiceCategory,
    val defaultPort: Int,
    val displayName: String,
) {
    KODI(ExternalServiceCategory.MEDIA_PLAYER, 8080, "Kodi"),
    VLC(ExternalServiceCategory.MEDIA_PLAYER, 9090, "VLC"),
    JACKETT(ExternalServiceCategory.INDEXER, 9117, "Jackett"),
    PROWLARR(ExternalServiceCategory.INDEXER, 9696, "Prowlarr"),
    ;

    companion object {
        fun fromString(value: String): ExternalServiceType = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: KODI
    }
}

data class ExternalService(
    val id: Long = 0,
    val name: String,
    val type: ExternalServiceType,
    val address: String,
    val username: String = "",
    val password: String = "",
    val isDefault: Boolean = false,
) {
    fun toEntity(): ExternalServiceEntity =
        ExternalServiceEntity(
            id = id,
            name = name,
            type = type.name,
            category = type.category.name,
            address = address,
            username = username,
            password = password,
            isDefault = isDefault,
        )

    companion object {
        fun fromEntity(entity: ExternalServiceEntity): ExternalService =
            ExternalService(
                id = entity.id,
                name = entity.name,
                type = ExternalServiceType.fromString(entity.type),
                address = entity.address,
                username = entity.username,
                password = entity.password,
                isDefault = entity.isDefault,
            )
    }
}
