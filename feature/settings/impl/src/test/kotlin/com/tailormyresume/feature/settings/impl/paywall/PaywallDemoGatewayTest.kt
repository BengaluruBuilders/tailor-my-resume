package com.tailormyresume.feature.settings.impl.paywall

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class PaywallDemoGatewayTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun demoPurchaseAddsLedgerRowAndBalance() = runTest {
        val credits = TestCreditsRepository()
        credits.sendLedger(listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.fromEpochSeconds(0))))
        val clock = TestClock()
        val gateway = CountingPaymentGateway(scripted = TestPaymentGateway())
        val viewModel = PaywallViewModel(gateway, credits, AccountCreditBalance(gateway, credits), clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onBuy()

        val row = credits.observeLedger().first().single { it.kind == CreditLedgerKind.PURCHASE }
        assertThat(row.kind).isEqualTo(CreditLedgerKind.PURCHASE)
        assertThat(row.productId).isEqualTo(ApplicationPack.APPLICATION_PACK_FIFTEEN)
        assertThat(row.createdAt).isEqualTo(clock.now())
        assertThat(viewModel.uiState.value.credits).isEqualTo(16)
    }
}
