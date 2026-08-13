package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullRootViewModel
import com.github.livingwithhippos.nullseek.NullViewModel
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentsViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelMobileModule =
    module {
        viewModel<NullRootViewModel>()
        viewModel<NullViewModel>()
        viewModel<TorrentsViewModel>()
    }

fun mobileModule() =
    module {
        includes(viewModelMobileModule)
    }
