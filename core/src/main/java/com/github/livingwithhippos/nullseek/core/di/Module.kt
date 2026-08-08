package com.github.livingwithhippos.nullseek.core.di

import com.github.livingwithhippos.nullseek.core.viewmodel.provider.ProviderViewModel
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.network.RestClientConfig
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel


val viewModelModule = module {
    viewModel<ProviderViewModel>()
}

val networkModule = module {
    single<TorboxClient> {TorboxClient.create(RestClientConfig(
        bearerToken = "",
        logLevel = "none"
    ))}
}

val appModule = module {
    includes(viewModelModule)
    includes(networkModule)
}