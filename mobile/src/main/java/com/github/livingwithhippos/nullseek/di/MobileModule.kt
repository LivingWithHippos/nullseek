package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullViewModel
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelMobileModule =
    module {
        viewModel<NullViewModel>()
        viewModel<UserViewModel>()
    }

fun mobileModule() =
    module {
        includes(viewModelMobileModule)
    }
