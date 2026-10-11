package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AccountCreditBalanceSourceTest {

    private val credits = TestCreditsRepository()
    private val staleGateway = TestPaymentGateway().withFreeCredits(5)
    private val balance = AccountCreditBalance(staleGateway, credits)

    private fun ledger(amount: Int) =
        listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, amount, null, null, Instant.fromEpochSeconds(1)))

    @Test
    fun theBalanceIsTheCreditsRepositoryNotTheGateway() = runTest {
        credits.sendLedger(ledger(2))

        assertThat(balance.cachedCredits()).isEqualTo(2)
        assertThat(balance.unusedCredits()).isEqualTo(2)
        assertThat(balance.observeCredits().first()).isEqualTo(2)
    }

    @Test
    fun anUnknownBalanceIsNullNotZero() = runTest {
        credits.sendUnknown()

        assertThat(balance.cachedCredits()).isNull()
        assertThat(balance.observeCredits().first()).isNull()
    }

    @Test
    fun theFreshReadRefreshesAnUnknownBalanceFirst() = runTest {
        credits.sendUnknown()
        credits.revealOnRefresh(ledger(4))

        assertThat(balance.unusedCredits()).isEqualTo(4)
        assertThat(credits.refreshCalls).isEqualTo(1)
    }

    @Test
    fun theFreshReadIsNullWhenTheBalanceStaysUnknown() = runTest {
        credits.sendUnknown()

        assertThat(balance.unusedCredits()).isNull()
    }

    @Test
    fun clearingStillEmptiesTheGatewayWallet() = runTest {
        balance.clearUnusedCredits()

        assertThat(staleGateway.entitlement().totalCredits).isEqualTo(0)
    }
}
