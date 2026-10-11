package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.CreditLedgerKind.FREE_GRANT
import com.tailormyresume.core.model.CreditLedgerKind.PURCHASE
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AccountCreditBalanceTest {

    private fun credits(vararg grants: Pair<CreditLedgerKind, Int>) = TestCreditsRepository().apply {
        sendLedger(grants.mapIndexed { index, (kind, amount) -> CreditLedgerEntry(kind, amount, null, null, Instant.fromEpochSeconds(index + 1L)) })
    }

    @Test
    fun unusedCreditsReadsTheLedgerBalance() = runTest {
        val balance = AccountCreditBalance(TestPaymentGateway(), credits(FREE_GRANT to 3))

        assertThat(balance.unusedCredits()).isEqualTo(3)
    }

    @Test
    fun unusedCreditsCountsPurchasedCreditsToo() = runTest {
        val balance = AccountCreditBalance(TestPaymentGateway(), credits(FREE_GRANT to 1, PURCHASE to 4))

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
