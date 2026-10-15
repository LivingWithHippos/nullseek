package com.github.livingwithhippos.nullseek.core.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider

@Database(
    entities = [
        NullProvider::class,
        ExternalServiceEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class NullDatabase : RoomDatabase() {
    abstract fun provider(): NullProviderDao

    abstract fun externalService(): ExternalServiceDao
}
