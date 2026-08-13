package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullRootViewModel
import com.github.livingwithhippos.nullseek.NullViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelMobileModule =
    module {
        viewModel<NullRootViewModel>()
        viewModel<NullViewModel>()
    }

fun mobileModule() =
    module {
        includes(viewModelMobileModule)
    }
