package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullRootViewModel
import com.github.livingwithhippos.nullseek.NullViewModel
import com.github.livingwithhippos.nullseek.data.remote.KodiRemoteClient
import com.github.livingwithhippos.nullseek.data.remote.VlcRemoteClient
import com.github.livingwithhippos.nullseek.data.repository.DownloadPreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import com.github.livingwithhippos.nullseek.ui.externalservices.ExternalServicesViewModel
import com.github.livingwithhippos.nullseek.ui.lists.ListsViewModel
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentDetailViewModel
import com.github.livingwithhippos.nullseek.ui.lists.usenet.UsenetDetailViewModel
import com.github.livingwithhippos.nullseek.ui.lists.web.WebDownloadDetailViewModel
import com.github.livingwithhippos.nullseek.ui.newdownload.NewDownloadViewModel
import com.github.livingwithhippos.nullseek.ui.settings.SettingsViewModel
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import okhttp3.OkHttpClient
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel
import java.util.concurrent.TimeUnit

val viewModelMobileModule =
    module {
        viewModel<NullRootViewModel>()
        viewModel<NullViewModel>()
        viewModel<ListsViewModel>()
        viewModel<NewDownloadViewModel>()
        viewModel<UserViewModel>()
        viewModel<TorrentDetailViewModel>()
        viewModel<WebDownloadDetailViewModel>()
        viewModel<UsenetDetailViewModel>()
        viewModel<SettingsViewModel>()
        viewModel<ExternalServicesViewModel>()
    }

val repositoryMobileModule =
    module {
        single {
            OkHttpClient
                .Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
        single { KodiRemoteClient(get()) }
        single { VlcRemoteClient(get()) }
        single { ExternalServicesRepository(get(), get(), get()) }
        single { DownloadPreferencesRepository(get()) }
    }

fun mobileModule() =
    module {
        includes(viewModelMobileModule, repositoryMobileModule)
    }
