package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullRootViewModel
import com.github.livingwithhippos.nullseek.NullViewModel
import com.github.livingwithhippos.nullseek.data.repository.DownloadPreferencesRepository
import com.github.livingwithhippos.nullseek.ui.lists.ListsViewModel
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentDetailViewModel
import com.github.livingwithhippos.nullseek.ui.lists.usenet.UsenetDetailViewModel
import com.github.livingwithhippos.nullseek.ui.lists.web.WebDownloadDetailViewModel
import com.github.livingwithhippos.nullseek.ui.newdownload.NewDownloadViewModel
import com.github.livingwithhippos.nullseek.ui.settings.SettingsViewModel
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

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
    }

val repositoryMobileModule =
    module {
        single { DownloadPreferencesRepository(get()) }
    }

fun mobileModule() =
    module {
        includes(viewModelMobileModule, repositoryMobileModule)
    }
