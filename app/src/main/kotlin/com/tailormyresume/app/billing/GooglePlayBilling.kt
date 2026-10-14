package com.tailormyresume.app.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.tailormyresume.app.auth.ForegroundActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlayBilling @Inject constructor(
    @ApplicationContext context: Context,
    private val foreground: ForegroundActivity,
) : PlayBilling {
    private val purchaseMutex = Mutex()
    private var inFlight: CompletableDeferred<PlayPurchaseResult>? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(::onPurchasesUpdated)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val unsolicited = MutableSharedFlow<PlayPurchase>(extraBufferCapacity = UNSOLICITED_BUFFER)

    override val unsolicitedPurchases: Flow<PlayPurchase> = unsolicited

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val waiting = inFlight?.takeIf { it.isActive }
        if (waiting != null) {
            waiting.complete(purchaseResultOf(result, purchases))
        } else if (result.isOk()) {
            purchases.orEmpty().flatMap { it.toPlayPurchases() }.forEach(unsolicited::tryEmit)
        }
    }

    override suspend fun productDetails(productId: String): PlayProduct? =
        loadProductDetails(productId)?.toPlayProduct()

    override suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlayPurchaseResult =
        purchaseMutex.withLock {
            val details = loadProductDetails(productId)
            val activity = foreground.current()
            if (details == null || activity == null) return PlayPurchaseResult.Failed
            val outcome = CompletableDeferred<PlayPurchaseResult>().also { inFlight = it }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
                )
                .setObfuscatedAccountId(obfuscatedAccountId)
                .build()
            val launched = client.launchBillingFlow(activity, params)
            if (launched.isOk()) {
                outcome.await()
            } else {
                inFlight = null
                purchaseResultOf(launched, null)
            }
        }

    override suspend fun ownedPurchases(): List<PlayPurchase> {
        if (!client.awaitConnection()) return emptyList()
        val query = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return client.queryPurchasesAsync(query).purchasesList.flatMap { it.toPlayPurchases() }
    }

    private suspend fun loadProductDetails(productId: String) = if (client.awaitConnection()) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
            .productDetailsList.orEmpty().firstOrNull()
    } else {
        null
    }
}

internal fun BillingResult.isOk() = responseCode == BillingClient.BillingResponseCode.OK

private const val UNSOLICITED_BUFFER = 8
