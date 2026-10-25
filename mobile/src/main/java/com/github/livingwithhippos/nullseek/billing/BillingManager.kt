package com.github.livingwithhippos.nullseek.billing

import android.app.Activity
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class EntitlementState(
    val isFossBuild: Boolean,
    val isPremiumUnlocked: Boolean,
    val formattedPrice: String? = null,
    val isBillingReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

interface BillingManager {
    val entitlementState: StateFlow<EntitlementState>

    suspend fun isPremiumUnlocked(): Boolean

    fun refreshPurchases()

    fun launchSubscriptionFlow(activity: Activity)
}
