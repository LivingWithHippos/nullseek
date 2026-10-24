package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

/** Mobile-only bindings. Shared repositories and ViewModels come from core's appModule(). */
fun mobileModule() =
    module {
        viewModel<NullViewModel>()
    }
