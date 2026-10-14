package com.tailormyresume.app.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

internal fun Purchase.toPlayPurchases(): List<PlayPurchase> {
    val state = when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> PlayPurchaseState.PURCHASED
        Purchase.PurchaseState.PENDING -> PlayPurchaseState.PENDING
        else -> return emptyList()
    }
    return products.map { productId -> PlayPurchase(productId, purchaseToken, state) }
}

internal fun purchaseResultOf(result: BillingResult, purchases: List<Purchase>?): PlayPurchaseResult {
    val purchase = purchases.orEmpty().flatMap(Purchase::toPlayPurchases).firstOrNull()
    return when {
        result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> PlayPurchaseResult.Cancelled
        result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PlayPurchaseResult.AlreadyOwned
        result.responseCode == BillingClient.BillingResponseCode.OK && purchase != null ->
            PlayPurchaseResult.Done(purchase)
        else -> PlayPurchaseResult.Failed
    }
}

internal fun ProductDetails.toPlayProduct(): PlayProduct? = oneTimePurchaseOfferDetails?.let { offer ->
    PlayProduct(name, offer.priceAmountMicros, offer.priceCurrencyCode)
}
