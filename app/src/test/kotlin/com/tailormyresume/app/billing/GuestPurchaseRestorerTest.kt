package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GuestPurchaseRestorerTest {
    @Test fun aGuestDoesNotStartBillingOrPurchaseRestoration() = runTest {
        val sessions = TestSessionRepository()
        sessions.saveAccount(SignInAccount("guest", "", "", AccountMode.ANONYMOUS))
        var paymentStarts = 0
        var billingStarts = 0
        val billing = FakePlayBilling()

        PurchaseRestorer(
            {
                paymentStarts++
                error("Guest must not initialize settlement")
            },
            {
                billingStarts++
                billing
            },
            sessions,
            backgroundScope,
        ).start()
        runCurrent()
        assertThat(paymentStarts).isEqualTo(0)
        assertThat(billingStarts).isEqualTo(0)
        assertThat(billing.unsolicitedPurchases.subscriptionCount.value).isEqualTo(0)
    }
}
