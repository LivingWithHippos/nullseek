package com.github.livingwithhippos.nullseek.core.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider

@Database(
    entities = [
        NullProvider::class,
    ],
    version = 1,
    exportSchema = true,
    /*
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
     */
)
abstract class NullDatabase : RoomDatabase() {
    abstract fun provider(): NullProviderDao
}
