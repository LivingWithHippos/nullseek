package com.github.livingwithhippos.nullseek.di

import com.github.livingwithhippos.nullseek.NullViewModel
import com.github.livingwithhippos.nullseek.billing.BillingManager
import com.github.livingwithhippos.nullseek.billing.EntitlementCache
import com.github.livingwithhippos.nullseek.billing.FlavorBillingManager
import com.github.livingwithhippos.nullseek.widget.TorboxWidgetRepository
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

/** Mobile-only bindings. Shared repositories and ViewModels come from core's appModule(). */
fun mobileModule() =
    module {
        single { EntitlementCache(get()) }
        single<BillingManager> { FlavorBillingManager(get(), get()) }
        single {
            TorboxWidgetRepository(
                context = get(),
                dataStore = get(),
                providerRepository = get(),
                torboxClient = get(),
                entitlementCache = get(),
            )
        }
        viewModel<NullViewModel>()
    }
