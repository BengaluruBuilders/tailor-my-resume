package com.tailormyresume.app.billing

import com.android.billingclient.api.Purchase

internal fun Purchase.toPlayPurchases(): List<PlayPurchase> {
    val state = when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> PlayPurchaseState.PURCHASED
        Purchase.PurchaseState.PENDING -> PlayPurchaseState.PENDING
        else -> return emptyList()
    }
    return products.map { productId -> PlayPurchase(productId, purchaseToken, state) }
}
