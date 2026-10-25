package com.github.livingwithhippos.nullseek.core.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.IndexerDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.StremioAddonDao
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity

@Database(
    entities = [
        NullProvider::class,
        ExternalServiceEntity::class,
        IndexerEntity::class,
        StremioAddonEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NullDatabase : RoomDatabase() {
    abstract fun provider(): NullProviderDao

    abstract fun externalService(): ExternalServiceDao

    abstract fun indexer(): IndexerDao

    abstract fun stremioAddon(): StremioAddonDao
}
