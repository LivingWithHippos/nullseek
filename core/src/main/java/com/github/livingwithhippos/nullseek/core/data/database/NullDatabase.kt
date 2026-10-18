package com.github.livingwithhippos.nullseek.core.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.IndexerDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider

@Database(
    entities = [
        NullProvider::class,
        ExternalServiceEntity::class,
        IndexerEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class NullDatabase : RoomDatabase() {
    abstract fun provider(): NullProviderDao

    abstract fun externalService(): ExternalServiceDao

    abstract fun indexer(): IndexerDao
}
