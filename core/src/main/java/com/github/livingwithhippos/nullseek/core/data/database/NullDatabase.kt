package com.github.livingwithhippos.nullseek.core.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.ImdbFavouriteDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.IndexerDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.StremioAddonDao
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.ImdbFavouriteEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity

@Database(
    entities = [
        NullProvider::class,
        ExternalServiceEntity::class,
        IndexerEntity::class,
        StremioAddonEntity::class,
        ImdbFavouriteEntity::class,
    ],
    version = 2,
    exportSchema = true,
    // the builder falls back to destructive migrations: every version bump needs a migration here,
    // or saved providers, services and addons are wiped
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class NullDatabase : RoomDatabase() {
    abstract fun provider(): NullProviderDao

    abstract fun externalService(): ExternalServiceDao

    abstract fun indexer(): IndexerDao

    abstract fun stremioAddon(): StremioAddonDao

    abstract fun imdbFavourite(): ImdbFavouriteDao
}
