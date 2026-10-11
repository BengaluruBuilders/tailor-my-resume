package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class AccountCreditBalanceCachedTest {

    @Test
    fun cachedCreditsNeverCallsEntitlement() = runTest {
        val gateway = EntitlementCountingGateway(TestPaymentGateway().withFreeCredits(3))
        val balance = AccountCreditBalance(gateway, ledger(3))

        assertThat(balance.cachedCredits()).isEqualTo(3)
        assertThat(gateway.entitlementCalls).isEqualTo(0)
    }

    @Test
    fun unknownLedgerDoesNotBecomeZeroCredits() = runTest {
        val credits = object : CreditsRepository by TestCreditsRepository() {
            override fun observeKnownBalance() = flowOf<Int?>(null)
            override suspend fun refreshConfirmed() = false
        }
        val balance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(4), credits)

        assertThat(balance.cachedCredits()).isNull()
        assertFailsWith<IllegalStateException> { balance.unusedCredits() }
    }

    @Test
    fun cachedCreditsReadsTheObservedEntitlement() = runTest {
        val balance = AccountCreditBalance(
            paymentGateway = TestPaymentGateway().withFreeCredits(2),
            creditsRepository = ledger(2),
        )

        assertThat(balance.cachedCredits()).isEqualTo(2)
    }
}

private fun ledger(credits: Int) = TestCreditsRepository().apply {
    sendLedger(listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, credits, null, null, Instant.parse("2026-10-12T09:00:00Z"))))
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
