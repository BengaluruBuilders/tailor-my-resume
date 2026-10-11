package com.tailormyresume.core.domain.gateway

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseOutcome
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.domain.offline.MockPackCatalogue
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import com.tailormyresume.core.testing.gateway.PaymentGatewayContractTest
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflinePaymentGatewayContractTest : PaymentGatewayContractTest() {

    override fun createPaymentGateway(): PaymentGateway = offlineGateway()

    @Test
    fun theCatalogueHoldsTheFiveFifteenAndFortyCreditPacks() = runTest {
        val gateway = offlineGateway()

        val packs = gateway.packs()

        assertThat(packs).isEqualTo(MockPackCatalogue.all)
        assertThat(packs.map { it.credits }).containsExactly(5, 15, 40).inOrder()
        assertThat(packs.first().priceInPaise).isEqualTo(19_900)
    }

    @Test
    fun aNewAccountHasTheFreeAllowanceAndNoPurchasedCredits() = runTest {
        val gateway = offlineGateway()

        val entitlement = gateway.entitlement()

        assertThat(entitlement.freeCredits).isEqualTo(1)
        assertThat(entitlement.purchasedCredits).isEqualTo(0)
        assertThat(entitlement.pendingPackIds).isEmpty()
    }

    @Test
    fun theSuccessOutcomeAddsTheCreditsOfTheBoughtPack() = runTest {
        val gateway = scripted(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Success)

        val result = gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        assertThat(result).isInstanceOf(PurchaseResult.Completed::class.java)
        assertThat(gateway.entitlement().purchasedCredits).isEqualTo(5)
    }

    @Test
    fun thePendingOutcomeAddsNoCreditsAndHoldsThePack() = runTest {
        val gateway = scripted(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Pending)

        val result = gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        assertThat(result).isInstanceOf(PurchaseResult.Pending::class.java)
        val entitlement = gateway.entitlement()
        assertThat(entitlement.purchasedCredits).isEqualTo(0)
        assertThat(entitlement.pendingPackIds).containsExactly(ApplicationPack.APPLICATION_PACK_FIVE)
    }

    @Test
    fun aConfirmedPurchaseClearsAPendingHoldOnTheSamePack() = runTest {
        val gateway = scripted(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Pending)
        gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)
        assertThat(gateway.entitlement().pendingPackIds).isNotEmpty()

        gateway.withOutcome(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Success)
        val result = gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        assertThat(result).isInstanceOf(PurchaseResult.Completed::class.java)
        val entitlement = gateway.entitlement()
        assertThat(entitlement.pendingPackIds).isEmpty()
        assertThat(entitlement.purchasedCredits).isEqualTo(5)
    }

    @Test
    fun theCancelledOutcomeAddsNoCredits() = runTest {
        val gateway = scripted(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Cancelled)

        val result = gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        assertThat(result).isEqualTo(PurchaseResult.Cancelled)
        assertThat(gateway.entitlement().purchasedCredits).isEqualTo(0)
    }

    @Test
    fun theFailedOutcomeReportsAReasonAndAddsNoCredits() = runTest {
        val gateway = offlineGateway()
            .withOutcome(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Failed)
            .withFailureReason(PurchaseFailureReason.PaymentDeclined)

        val result = gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        assertThat(result).isInstanceOf(PurchaseResult.Failed::class.java)
        assertThat((result as PurchaseResult.Failed).reason).isEqualTo(PurchaseFailureReason.PaymentDeclined)
        assertThat(gateway.entitlement().purchasedCredits).isEqualTo(0)
    }

    @Test
    fun eachFailureReasonCanBeForced() = runTest {
        PurchaseFailureReason.entries.forEach { reason ->
            val gateway = offlineGateway()
                .withOutcome(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Failed)
                .withFailureReason(reason)

            val result = gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE) as PurchaseResult.Failed

            assertThat(result.reason).isEqualTo(reason)
        }
    }

    @Test
    fun theSameInputAlwaysProducesTheSameResult() = runTest {
        val first = scripted(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Pending)
        val second = scripted(ApplicationPack.APPLICATION_PACK_FIVE, PurchaseOutcome.Pending)

        assertThat(second.purchase(ApplicationPack.APPLICATION_PACK_FIVE))
            .isEqualTo(first.purchase(ApplicationPack.APPLICATION_PACK_FIVE))
    }

    @Test
    fun restoreReturnsWhatTheAccountAlreadyOwns() = runTest {
        val gateway = offlineGateway()
        gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        val restored = gateway.restorePurchases()

        assertThat(restored).isEqualTo(gateway.entitlement())
        assertThat(restored.purchasedCredits).isEqualTo(5)
    }

    @Test
    fun buyingTheSameConsumablePackTwiceGrantsItTwice() = runTest {
        val gateway = offlineGateway()

        gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)
        gateway.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        assertThat(gateway.entitlement().purchasedCredits).isEqualTo(10)
    }

    private fun offlineGateway(): OfflinePaymentGateway =
        OfflinePaymentGateway(TestMockStateStore(), NoMockLatency, TestClock(), TestIdGenerator("order"))

    private fun scripted(
        packId: String,
        outcome: PurchaseOutcome,
    ): OfflinePaymentGateway = offlineGateway().withOutcome(packId, outcome)
}
