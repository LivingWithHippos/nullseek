package com.github.livingwithhippos.nullseek.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import com.github.livingwithhippos.nullseek.core.data.database.NullDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.IndexerDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.repository.ExternalServiceRepository
import com.github.livingwithhippos.nullseek.core.data.repository.ExternalServiceRepositoryImpl
import com.github.livingwithhippos.nullseek.core.data.repository.IndexerRepository
import com.github.livingwithhippos.nullseek.core.data.repository.IndexerRepositoryImpl
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepositoryImpl
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.network.TorboxAuthState
import com.github.livingwithhippos.torbox.network.TorboxClientConfig
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create

private fun networkModule(logLevel: String = "none") =
    module {
        single<TorboxClient> {
            TorboxClient.create(
                TorboxClientConfig(
                    logLevel = logLevel,
                ),
                TorboxAuthState(),
            )
        }
    }

private fun database(context: Context): NullDatabase =
    Room
        .databaseBuilder(
            context,
            NullDatabase::class.java,
            "null.db",
        ).fallbackToDestructiveMigration(dropAllTables = true)
        .build()

private fun providerDao(db: NullDatabase): NullProviderDao = db.provider()

private fun externalServiceDao(db: NullDatabase): ExternalServiceDao = db.externalService()

private fun indexerDao(db: NullDatabase): IndexerDao = db.indexer()

private fun indexerRepository(dao: IndexerDao): IndexerRepository = IndexerRepositoryImpl(dao)

private fun providerRepository(dao: NullProviderDao): NullProviderRepository = NullProviderRepositoryImpl(dao)

private fun externalServiceRepository(dao: ExternalServiceDao): ExternalServiceRepository = ExternalServiceRepositoryImpl(dao)

private fun datastore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("settings") },
    )

private val storageModule =
    module {
        single { create(::datastore) }

        single { create(::database) }

        single { create(::providerDao) }

        single { create(::providerRepository) }

        single { create(::externalServiceDao) }

        single { create(::externalServiceRepository) }

        single { create(::indexerDao) }

        single { create(::indexerRepository) }
    }

fun appModule(debug: Boolean) =
    module {
        includes(networkModule(logLevel = if (debug) "body" else "none"))
        includes(storageModule)
    }
