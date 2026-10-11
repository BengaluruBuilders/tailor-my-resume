package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.domain.TailoringCreditSpend
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AccountCreditBalanceTest {

    @Test
    fun displayedBalanceFollowsLedgerSpendEvenWhenGatewayIsStale() = runTest {
        val gateway = TestPaymentGateway().withFreeCredits(3)
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
        val gateway = TestPaymentGateway().withFreeCredits(3)
        val ledger = TestCreditsRepository()
        ledger.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-12T09:00:00Z")))
        val balance = AccountCreditBalance(gateway, ledger)

        assertThat(balance.unusedCredits()).isEqualTo(1)
    }

    @Test
    fun unusedCreditsCountsPurchasedCreditsToo() = runTest {
        val gateway = TestPaymentGateway().withFreeCredits(0)
        gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)
        val ledger = TestCreditsRepository()
        ledger.record(CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, null, "application_pack_5", Instant.parse("2026-10-12T09:00:00Z")))
        val balance = AccountCreditBalance(gateway, ledger)

        assertThat(balance.unusedCredits()).isEqualTo(5)
    }

    @Test
    fun clearingTakesTheRealBalanceToZero() = runTest {
        val gateway = TestPaymentGateway().withFreeCredits(0)
        gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)
        val balance = AccountCreditBalance(gateway, TestCreditsRepository())

        balance.clearUnusedCredits()

        assertThat(gateway.entitlement().totalCredits).isEqualTo(0)
    }

    @Test
    fun clearingAnAlreadyEmptyBalanceDoesNothing() = runTest {
        val gateway = TestPaymentGateway().withFreeCredits(0)
        val balance = AccountCreditBalance(gateway, TestCreditsRepository())

        balance.clearUnusedCredits()

        assertThat(gateway.entitlement().totalCredits).isEqualTo(0)
    }

    @Test
    fun clearingAGatewayThatNeverDropsFailsInsteadOfClaimingSuccess() = runTest {
        val balance = AccountCreditBalance(StuckPaymentGateway(), TestCreditsRepository())

        val thrown = runCatching { balance.clearUnusedCredits() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
    }
}

private class StuckPaymentGateway : PaymentGateway {

    override suspend fun packs(): List<ApplicationPack> = emptyList()

    override suspend fun purchase(packId: String): PurchaseResult = PurchaseResult.Failed(
        reason = PurchaseFailureReason.PurchaseUnavailable,
        entitlement = entitlement(),
    )

    override suspend fun entitlement(): PurchaseEntitlement = PurchaseEntitlement(
        freeCredits = 4,
        purchasedCredits = 0,
        pendingPackIds = emptyList(),
    )

    override suspend fun restorePurchases(): PurchaseEntitlement = entitlement()

    override suspend fun unlock(applicationId: String): CreditSpend = CreditSpend.Spent(entitlement())

    override suspend fun clearCredits(): PurchaseEntitlement = entitlement()
}
