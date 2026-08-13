package com.github.livingwithhippos.nullseek.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import com.github.livingwithhippos.nullseek.core.data.database.NullDatabase
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
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
        ).build()

private fun providerDao(db: NullDatabase): NullProviderDao = db.provider()

private fun providerRepository(dao: NullProviderDao): NullProviderRepository = NullProviderRepositoryImpl(dao)

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
    }

fun appModule(debug: Boolean) =
    module {
        includes(networkModule(logLevel = if (debug) "info" else "none"))
        includes(storageModule)
    }
