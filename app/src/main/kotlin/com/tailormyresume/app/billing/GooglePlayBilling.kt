package com.tailormyresume.app.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlayBilling @Inject constructor(
    @ApplicationContext context: Context,
) : PlayBilling {
    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(::onPurchasesUpdated)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val unsolicited = MutableSharedFlow<PlayPurchase>(extraBufferCapacity = UNSOLICITED_BUFFER)

    override val unsolicitedPurchases: Flow<PlayPurchase> = unsolicited

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.isOk()) purchases.orEmpty().flatMap { it.toPlayPurchases() }.forEach(unsolicited::tryEmit)
    }

    override suspend fun ownedPurchases(): List<PlayPurchase> {
        if (!client.awaitConnection()) return emptyList()
        val query = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return client.queryPurchasesAsync(query).purchasesList.flatMap { it.toPlayPurchases() }
    }
}

internal fun BillingResult.isOk() = responseCode == BillingClient.BillingResponseCode.OK

private const val UNSOLICITED_BUFFER = 8
