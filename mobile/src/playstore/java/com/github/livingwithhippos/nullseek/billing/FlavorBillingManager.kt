package com.github.livingwithhippos.nullseek.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.github.livingwithhippos.nullseek.BuildConfig
import com.github.livingwithhippos.nullseek.widget.TorboxWidgetRepository
import com.github.livingwithhippos.nullseek.widget.TorboxWidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat

class FlavorBillingManager(
    private val context: Context,
    private val entitlementCache: EntitlementCache,
) : BillingManager,
    PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _entitlementState =
        MutableStateFlow(
            EntitlementState(
                isFossBuild = false,
                isPremiumUnlocked = false,
                isLoading = true,
            ),
        )
    override val entitlementState: StateFlow<EntitlementState> = _entitlementState.asStateFlow()

    @Volatile
    private var cachedProductDetails: ProductDetails? = null

    private val billingClient: BillingClient =
        BillingClient
            .newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            ).build()

    init {
        scope.launch {
            val cachedUnlocked = entitlementCache.isUnlocked()
            _entitlementState.update {
                it.copy(isPremiumUnlocked = cachedUnlocked)
            }
            connectAndQuery()
        }
    }

    override suspend fun isPremiumUnlocked(): Boolean = entitlementCache.isUnlocked()

    override fun refreshPurchases() {
        scope.launch {
            if (billingClient.isReady) {
                queryProductAndPurchases()
            } else {
                connectAndQuery()
            }
        }
    }

    override fun launchSubscriptionFlow(activity: Activity) {
        val productDetails = cachedProductDetails
        if (!billingClient.isReady || productDetails == null) {
            _entitlementState.update {
                it.copy(errorMessage = "Google Play Billing is not ready yet")
            }
            refreshPurchases()
            return
        }

        val offerToken =
            productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken

        if (offerToken.isNullOrBlank()) {
            _entitlementState.update {
                it.copy(errorMessage = "No subscription offer available")
            }
            return
        }

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()

        val flowParams =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    handlePurchases(purchases.orEmpty())
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _entitlementState.update { it.copy(errorMessage = null) }
            }

            else -> {
                logcat("FlavorBillingManager") {
                    "Purchase failed: ${billingResult.responseCode} - ${billingResult.debugMessage}"
                }
                _entitlementState.update {
                    it.copy(errorMessage = billingResult.debugMessage.ifBlank { "Purchase failed" })
                }
            }
        }
    }

    private fun connectAndQuery() {
        _entitlementState.update { it.copy(isLoading = true, errorMessage = null) }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        scope.launch {
                            queryProductAndPurchases()
                        }
                    } else {
                        _entitlementState.update {
                            it.copy(
                                isBillingReady = false,
                                isLoading = false,
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _entitlementState.update {
                        it.copy(isBillingReady = false)
                    }
                }
            },
        )
    }

    private suspend fun queryProductAndPurchases() {
        val productId = BuildConfig.SUBSCRIPTION_PRODUCT_ID
        if (productId.isNotBlank()) {
            val productParams =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product
                                .newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        ),
                    ).build()

            val detailsResult = billingClient.queryProductDetails(productParams)
            val details = detailsResult.productDetailsList?.firstOrNull()
            cachedProductDetails = details
            val price =
                details
                    ?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice

            _entitlementState.update {
                it.copy(
                    formattedPrice = price,
                    isBillingReady = true,
                )
            }
        }

        val purchasesParams =
            QueryPurchasesParams
                .newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

        val purchasesResult = billingClient.queryPurchasesAsync(purchasesParams)
        if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(purchasesResult.purchasesList)
        } else {
            _entitlementState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        var hasActiveSub = false
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                hasActiveSub = true
                if (!purchase.isAcknowledged) {
                    val ackParams =
                        AcknowledgePurchaseParams
                            .newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    billingClient.acknowledgePurchase(ackParams)
                }
            }
        }

        val previous = entitlementCache.isUnlocked()
        entitlementCache.setUnlocked(hasActiveSub)
        _entitlementState.update {
            it.copy(
                isPremiumUnlocked = hasActiveSub,
                isBillingReady = true,
                isLoading = false,
                errorMessage = null,
            )
        }

        if (previous != hasActiveSub) {
            if (hasActiveSub) {
                TorboxWidgetUpdateWorker.schedulePeriodic(context)
                TorboxWidgetUpdateWorker.enqueueImmediate(context)
            } else {
                TorboxWidgetRepository.updateAllWidgets(context)
            }
        }
    }
}
