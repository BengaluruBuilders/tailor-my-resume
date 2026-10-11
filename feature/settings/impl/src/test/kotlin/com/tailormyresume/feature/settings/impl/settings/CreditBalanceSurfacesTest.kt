package com.tailormyresume.feature.settings.impl.settings

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.settings.impl.credits.CreditsScreen
import com.tailormyresume.feature.settings.impl.credits.CreditsViewModel
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountSheetContent
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountUiState
import com.tailormyresume.feature.settings.impl.delete.DeleteFixture
import com.tailormyresume.feature.settings.impl.paywall.PaywallActions
import com.tailormyresume.feature.settings.impl.paywall.PaywallScreen
import com.tailormyresume.feature.settings.impl.paywall.PaywallViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class CreditBalanceSurfacesTest {
    private val dispatcher = MainDispatcherRule()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(dispatcher).around(compose)

    private var surface by mutableIntStateOf(0)

    private suspend fun show(gateway: PaymentGateway) {
        val fixture = SettingsFixture(payment = gateway)
        fixture.credits.sendLedger(listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 99, null, null, Instant.fromEpochSeconds(0))))
        val balance = AccountCreditBalance(gateway)
        val settings = fixture.viewModel()
        val credits = CreditsViewModel(fixture.credits, balance, TestApplicationRepository())
        val paywall = PaywallViewModel(gateway, fixture.credits, balance, TestClock())
        val delete = DeleteFixture(payment = gateway).seed().viewModel()
        compose.setContent {
            val settingsState by settings.uiState.collectAsState()
            val creditsState by credits.uiState.collectAsState()
            val paywallState by paywall.uiState.collectAsState()
            val deleteState by delete.uiState.collectAsState()
            TmrTheme {
                when (surface) {
                    0 -> SettingsScreen(settingsState, "test", SettingsActions({}, {}, {}, {}, {}, {}, {}, {}))
                    1 -> CreditsScreen(creditsState, {})
                    2 -> PaywallScreen(paywallState, PaywallActions({}, {}, {}))
                    3 -> (deleteState as? DeleteAccountUiState.Content)?.let { DeleteAccountSheetContent(it, {}, {}, {}) }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertEverySurface(credits: Int?) {
        val labels = if (credits == null) {
            listOf("Balance unavailable", "Balance unavailable", "Balance unavailable", "any unused credits")
        } else {
            listOf("$credits left", credits.toString(), "$credits credits left", "$credits unused credits")
        }
        labels.forEachIndexed { index, label ->
            compose.runOnIdle { surface = index }
            compose.waitForIdle()
            compose.onNodeWithText(label, substring = index == 3, ignoreCase = true).assertExists()
        }
    }

    @Test
    fun demoWalletUpdatesAllFourSurfacesDespiteDifferentLedgerTotal() = runTest {
        val gateway = TestPaymentGateway().withFreeCredits(4)
        show(gateway)
        assertEverySurface(4)

        gateway.unlock("application-1")
        compose.waitForIdle()

        assertEverySurface(3)
    }

    @Test
    fun serverWalletWithoutACacheStaysUnknownUntilTheWalletArrives() = runTest {
        val gateway = ObservedWalletGateway()
        show(gateway)
        assertEverySurface(null)

        gateway.wallet.value = PurchaseEntitlement(0, 4, emptyList())
        compose.waitForIdle()

        assertEverySurface(4)
    }
}

private class ObservedWalletGateway : PaymentGateway by TestPaymentGateway() {
    val wallet = MutableStateFlow(PurchaseEntitlement(0, 0, emptyList(), hasKnownBalance = false))

    override fun observeEntitlement() = wallet

    override suspend fun entitlement(): PurchaseEntitlement = wallet.value
}
