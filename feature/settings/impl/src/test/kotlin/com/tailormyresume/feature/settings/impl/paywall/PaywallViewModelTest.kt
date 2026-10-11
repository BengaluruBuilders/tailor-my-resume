package com.tailormyresume.feature.settings.impl.paywall

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseOutcome
import com.tailormyresume.core.domain.offline.MockPackCatalogue
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PaywallViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = TestClock()
    private val fifteen = ApplicationPack.APPLICATION_PACK_FIFTEEN

    private fun viewModel(
        gateway: CountingPaymentGateway = CountingPaymentGateway(),
        credits: com.tailormyresume.core.data.repository.CreditsRepository = RecordingCreditsRepository(),
    ) = PaywallViewModel(gateway, credits, clock)

    private fun TestScope.keepSubscribed(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private fun TestScope.collectEvents(viewModel: PaywallViewModel): List<PaywallEvent> {
        val events = mutableListOf<PaywallEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return events
    }

    @Test
    fun packsComeFromGatewayAndFifteenIsPreselected() = runTest {
        val viewModel = viewModel()
        keepSubscribed(viewModel.uiState)

        assertThat(viewModel.uiState.value.packs).isEqualTo(MockPackCatalogue.all)
        assertThat(viewModel.uiState.value.selectedPackId).isEqualTo(fifteen)
    }

    @Test
    fun selectingAnotherPackChangesTheBuyTarget() = runTest {
        val gateway = CountingPaymentGateway()
        val viewModel = viewModel(gateway)
        keepSubscribed(viewModel.uiState)

        viewModel.onSelectPack(ApplicationPack.APPLICATION_PACK_FIVE)
        viewModel.onBuy()

        assertThat(gateway.purchasedPackIds).containsExactly(ApplicationPack.APPLICATION_PACK_FIVE)
    }

    @Test
    fun packsLoadFailureToastsAndRetriesOnBuy() = runTest {
        val gateway = CountingPaymentGateway(packsFailures = 1)
        val viewModel = viewModel(gateway)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)

        assertThat(events).containsExactly(PaywallEvent.PricesUnavailable)
        assertThat(viewModel.uiState.value.packs).isEmpty()

        viewModel.onBuy()

        assertThat(gateway.packsCalls).isEqualTo(2)
        assertThat(gateway.purchaseCalls).isEqualTo(0)
        assertThat(viewModel.uiState.value.packs).isEqualTo(MockPackCatalogue.all)
        assertThat(events).containsExactly(PaywallEvent.PricesUnavailable)
    }

    @Test
    fun secondTapWhilePurchasingDoesNothingAndPurchasesOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        val gateway = CountingPaymentGateway(purchaseGate = gate)
        val credits = RecordingCreditsRepository()
        val viewModel = viewModel(gateway, credits)
        keepSubscribed(viewModel.uiState)

        viewModel.onBuy()
        viewModel.onBuy()

        assertThat(viewModel.uiState.value.busy).isTrue()
        assertThat(gateway.purchaseCalls).isEqualTo(1)
        gate.complete(Unit)
        assertThat(viewModel.uiState.value.busy).isFalse()
        assertThat(gateway.purchaseCalls).isEqualTo(1)
        assertThat(credits.recorded).hasSize(1)
    }

    @Test
    fun completedRefreshesCreditsAndEmitsAdded() = runTest {
        val credits = RecordingCreditsRepository()
        val viewModel = viewModel(credits = credits)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)

        viewModel.onBuy()

        assertThat(events).containsExactly(PaywallEvent.Added(15))
        assertThat(credits.calls).containsExactly("refresh", "record", "refresh").inOrder()
        val entry = credits.recorded.single()
        assertThat(entry.kind).isEqualTo(CreditLedgerKind.PURCHASE)
        assertThat(entry.amount).isEqualTo(15)
        assertThat(entry.productId).isEqualTo(fifteen)
        assertThat(entry.createdAt).isEqualTo(clock.now())
    }

    @Test
    fun failedShowsPaymentFailedAndTryAgainBuysAgain() = runTest {
        val gateway = CountingPaymentGateway()
        gateway.scripted.withOutcome(fifteen, PurchaseOutcome.Failed).withFailureReason(PurchaseFailureReason.PaymentDeclined)
        val credits = RecordingCreditsRepository()
        val viewModel = viewModel(gateway, credits)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)

        viewModel.onBuy()

        assertThat(events).containsExactly(PaywallEvent.PaymentFailed)
        assertThat(viewModel.uiState.value.busy).isFalse()
        assertThat(credits.calls).containsExactly("refresh")

        gateway.scripted.withOutcome(fifteen, PurchaseOutcome.Success)
        viewModel.onBuy()

        assertThat(gateway.purchasedPackIds).containsExactly(fifteen, fifteen)
        assertThat(events).containsExactly(PaywallEvent.PaymentFailed, PaywallEvent.Added(15)).inOrder()
    }

    @Test
    fun cancelledChangesNothing() = runTest {
        val gateway = CountingPaymentGateway()
        gateway.scripted.withOutcome(fifteen, PurchaseOutcome.Cancelled)
        val credits = RecordingCreditsRepository()
        val viewModel = viewModel(gateway, credits)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)
        val before = viewModel.uiState.value

        viewModel.onBuy()

        assertThat(gateway.purchaseCalls).isEqualTo(1)
        assertThat(events).isEmpty()
        assertThat(viewModel.uiState.value).isEqualTo(before)
        assertThat(credits.calls).containsExactly("refresh")
    }

    @Test
    fun pendingToastsAndAddsNothing() = runTest {
        val gateway = CountingPaymentGateway()
        gateway.scripted.withOutcome(fifteen, PurchaseOutcome.Pending)
        val credits = RecordingCreditsRepository()
        val viewModel = viewModel(gateway, credits)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)

        viewModel.onBuy()

        assertThat(events).containsExactly(PaywallEvent.Pending)
        assertThat(credits.recorded).isEmpty()
        assertThat(credits.calls).containsExactly("refresh")
        assertThat(viewModel.uiState.value.credits).isEqualTo(0)
        assertThat(viewModel.uiState.value.busy).isFalse()
    }

    @Test
    fun restoreWithNothingToastsNoPreviousPurchases() = runTest {
        val gateway = CountingPaymentGateway()
        val viewModel = viewModel(gateway)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)

        viewModel.onRestore()

        assertThat(gateway.restoreCalls).isEqualTo(1)
        assertThat(events).containsExactly(PaywallEvent.NothingToRestore)
    }

    @Test
    fun restoreThatAddsCreditsToastsAddedAndRefreshes() = runTest {
        val credits = RemoteStyleCreditsRepository(initialBalance = 1)
        val viewModel = viewModel(credits = credits)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)
        credits.serverBalance = 4

        viewModel.onRestore()

        assertThat(events).containsExactly(PaywallEvent.Added(3))
        assertThat(credits.refreshCalls).isEqualTo(2)
        assertThat(viewModel.uiState.value.credits).isEqualTo(4)
    }

    @Test
    fun restoreFailureToasts() = runTest {
        val gateway = CountingPaymentGateway(restoreFailure = IOException("offline"))
        val viewModel = viewModel(gateway)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)

        viewModel.onRestore()

        assertThat(events).containsExactly(PaywallEvent.RestoreFailed)
        assertThat(viewModel.uiState.value.busy).isFalse()
    }

    @Test
    fun restoreSecondTapWhileRestoringIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        val gateway = CountingPaymentGateway(restoreGate = gate)
        val viewModel = viewModel(gateway)
        keepSubscribed(viewModel.uiState)

        viewModel.onRestore()
        viewModel.onRestore()
        viewModel.onBuy()

        assertThat(gateway.restoreCalls).isEqualTo(1)
        assertThat(gateway.purchaseCalls).isEqualTo(0)
        gate.complete(Unit)
    }

    @Test
    fun enteringPaywallStartsNoPurchase() = runTest {
        val gateway = CountingPaymentGateway()
        val viewModel = viewModel(gateway)
        keepSubscribed(viewModel.uiState)

        assertThat(gateway.purchaseCalls).isEqualTo(0)
        assertThat(gateway.restoreCalls).isEqualTo(0)
        assertThat(gateway.packsCalls).isEqualTo(1)
    }

    @Test
    fun balanceChangesOnlyAfterRepositoryRefresh() = runTest {
        val credits = RemoteStyleCreditsRepository(initialBalance = 1)
        val viewModel = viewModel(credits = credits)
        keepSubscribed(viewModel.uiState)
        val events = collectEvents(viewModel)
        val gate = CompletableDeferred<Unit>()
        credits.refreshGate = gate
        credits.serverBalance = 16

        viewModel.onBuy()

        assertThat(credits.recordCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.credits).isEqualTo(1)
        assertThat(events).isEmpty()
        gate.complete(Unit)
        assertThat(viewModel.uiState.value.credits).isEqualTo(16)
        assertThat(events).containsExactly(PaywallEvent.Added(15))
    }
}
