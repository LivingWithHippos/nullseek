package com.github.livingwithhippos.nullseek.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlavorBillingManager(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") private val entitlementCache: EntitlementCache,
) : BillingManager {
    private val _entitlementState =
        MutableStateFlow(
            EntitlementState(
                isFossBuild = true,
                isPremiumUnlocked = true,
                isBillingReady = true,
            ),
        )
    override val entitlementState: StateFlow<EntitlementState> = _entitlementState.asStateFlow()

    override suspend fun isPremiumUnlocked(): Boolean = true

    override fun refreshPurchases() {
        // Always unlocked in the FOSS edition.
    }

    override fun launchSubscriptionFlow(activity: Activity) {
        // No subscription flow in the FOSS edition.
    }
}
