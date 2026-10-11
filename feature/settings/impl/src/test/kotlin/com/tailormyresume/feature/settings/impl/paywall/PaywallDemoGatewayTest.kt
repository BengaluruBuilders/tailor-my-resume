package com.tailormyresume.feature.settings.impl.paywall

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PaywallDemoGatewayTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun demoPurchaseAddsLedgerRowAndBalance() = runTest {
        val credits = TestCreditsRepository()
        val clock = TestClock()
        val viewModel = PaywallViewModel(CountingPaymentGateway(), credits, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onBuy()

        assertThat(credits.observeBalance().first()).isEqualTo(15)
        val row = credits.observeLedger().first().single()
        assertThat(row.kind).isEqualTo(CreditLedgerKind.PURCHASE)
        assertThat(row.productId).isEqualTo(ApplicationPack.APPLICATION_PACK_FIFTEEN)
        assertThat(row.createdAt).isEqualTo(clock.now())
        assertThat(viewModel.uiState.value.credits).isEqualTo(15)
    }
}
