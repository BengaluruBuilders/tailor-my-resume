package com.tailormyresume.app.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayBillingMappingTest {
    private fun result(code: Int) = BillingResult.newBuilder().setResponseCode(code).build()

    @Test
    fun itemAlreadyOwnedMapsToAlreadyOwned() {
        val outcome = purchaseResultOf(result(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED), null)

        assertThat(outcome).isEqualTo(PlayPurchaseResult.AlreadyOwned)
    }

    @Test
    fun userCanceledMapsToCancelled() {
        val outcome = purchaseResultOf(result(BillingClient.BillingResponseCode.USER_CANCELED), null)

        assertThat(outcome).isEqualTo(PlayPurchaseResult.Cancelled)
    }

    @Test
    fun otherErrorsMapToFailed() {
        val outcome = purchaseResultOf(result(BillingClient.BillingResponseCode.ERROR), null)

        assertThat(outcome).isEqualTo(PlayPurchaseResult.Failed)
    }
}
