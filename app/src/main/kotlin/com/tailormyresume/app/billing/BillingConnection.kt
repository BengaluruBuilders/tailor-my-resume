package com.tailormyresume.app.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun BillingClient.awaitConnection(): Boolean {
    if (isReady) return true
    return suspendCancellableCoroutine { continuation ->
        startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result.isOk())
                }

                override fun onBillingServiceDisconnected() = Unit
            },
        )
    }
}
