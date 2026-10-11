package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AccountCreditBalanceCachedTest {

    private val credits = TestCreditsRepository()

    @Test
    fun cachedCreditsNeverRefreshesTheBalance() = runTest {
        credits.sendLedger(listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 3, null, null, Instant.fromEpochSeconds(1))))
        val balance = AccountCreditBalance(TestPaymentGateway(), credits)

        assertThat(balance.cachedCredits()).isEqualTo(3)
        assertThat(credits.refreshCalls).isEqualTo(0)
    }

    @Test
    fun cachedCreditsReadsTheObservedLedgerBalance() = runTest {
        credits.sendLedger(listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 2, null, null, Instant.fromEpochSeconds(1))))
        val balance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(9), credits)

        assertThat(balance.cachedCredits()).isEqualTo(2)
    }
}
