package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class AccountCreditBalanceCachedTest {

    @Test
    fun cachedCreditsNeverCallsEntitlement() = runTest {
        val gateway = EntitlementCountingGateway(TestPaymentGateway().withFreeCredits(3))
        val balance = AccountCreditBalance(paymentGateway = gateway)

        assertThat(balance.cachedCredits()).isEqualTo(3)
        assertThat(gateway.entitlementCalls).isEqualTo(0)
    }

    @Test
    fun unknownWalletDoesNotBecomeZeroCredits() = runTest {
        val unknown = PurchaseEntitlement(0, 0, emptyList(), hasKnownBalance = false)
        val gateway = object : PaymentGateway by TestPaymentGateway() {
            override fun observeEntitlement() = flowOf(unknown)
            override suspend fun entitlement(): PurchaseEntitlement = error("wallet unavailable")
        }
        val balance = AccountCreditBalance(gateway)

        assertThat(balance.cachedCredits()).isNull()
        assertFailsWith<IllegalStateException> { balance.unusedCredits() }
    }

    @Test
    fun cachedCreditsReadsTheObservedEntitlement() = runTest {
        val balance = AccountCreditBalance(
            paymentGateway = TestPaymentGateway().withFreeCredits(2),
        )

        assertThat(balance.cachedCredits()).isEqualTo(2)
    }
}

private class EntitlementCountingGateway(
    private val delegate: PaymentGateway,
) : PaymentGateway by delegate {

    var entitlementCalls = 0

    override suspend fun entitlement(): PurchaseEntitlement {
        entitlementCalls++
        return delegate.entitlement()
    }
}
