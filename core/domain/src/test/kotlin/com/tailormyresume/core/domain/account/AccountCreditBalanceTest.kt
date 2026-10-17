package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.TailoringCreditSpend
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestAccountWallet
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AccountCreditBalanceTest {

    @Test
    fun displayedBalanceFollowsLedgerSpendEvenWhenGatewayIsStale() = runTest {
        val gateway = TestAccountWallet().withFreeCredits(3)
        val ledger = TestCreditsRepository()
        val balance = AccountCreditBalance(gateway, ledger)
        ledger.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-12T09:00:00Z")))

        assertThat(balance.observeCredits().first()).isEqualTo(1)
        ledger.record(TailoringCreditSpend.spendEntry("app-1", "run-1", kotlin.time.Clock.System))

        assertThat(balance.cachedCredits()).isEqualTo(0)
        assertThat(ledger.observeLedger().first().filter { it.kind == CreditLedgerKind.SPEND }).hasSize(1)
        assertThat(gateway.entitlement().totalCredits).isEqualTo(3)
    }

    @Test
    fun unusedCreditsReadsTheRefreshedLedgerBalance() = runTest {
        val gateway = TestAccountWallet().withFreeCredits(3)
        val ledger = TestCreditsRepository()
        ledger.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-12T09:00:00Z")))
        val balance = AccountCreditBalance(gateway, ledger)

        assertThat(balance.unusedCredits()).isEqualTo(1)
    }

    @Test
    fun unusedCreditsCountsPurchasedCreditsToo() = runTest {
        val gateway = TestAccountWallet().withFreeCredits(0)
        gateway.withHistoricalPurchase("application_pack_5")
        val ledger = TestCreditsRepository()
        ledger.record(CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, null, "application_pack_5", Instant.parse("2026-10-12T09:00:00Z")))
        val balance = AccountCreditBalance(gateway, ledger)

        assertThat(balance.unusedCredits()).isEqualTo(5)
    }

    @Test
    fun clearingTakesTheRealBalanceToZero() = runTest {
        val gateway = TestAccountWallet().withFreeCredits(0)
        gateway.withHistoricalPurchase("application_pack_5")
        val balance = AccountCreditBalance(gateway, TestCreditsRepository())

        balance.clearUnusedCredits()

        assertThat(gateway.entitlement().totalCredits).isEqualTo(0)
    }

    @Test
    fun clearingAnAlreadyEmptyBalanceDoesNothing() = runTest {
        val gateway = TestAccountWallet().withFreeCredits(0)
        val balance = AccountCreditBalance(gateway, TestCreditsRepository())

        balance.clearUnusedCredits()

        assertThat(gateway.entitlement().totalCredits).isEqualTo(0)
    }

    @Test
    fun clearingAGatewayThatNeverDropsFailsInsteadOfClaimingSuccess() = runTest {
        val balance = AccountCreditBalance(StuckAccountWallet(), TestCreditsRepository())

        val thrown = runCatching { balance.clearUnusedCredits() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
    }
}

private class StuckAccountWallet : AccountWallet {

    override suspend fun entitlement(): PurchaseEntitlement = PurchaseEntitlement(
        freeCredits = 4,
        purchasedCredits = 0,
        pendingPackIds = emptyList(),
    )

    override suspend fun clearCredits(): PurchaseEntitlement = entitlement()
}
