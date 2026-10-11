package com.tailormyresume.feature.settings.impl.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.domain.account.DeleteAccountUseCase
import com.tailormyresume.core.domain.account.ExportAccountDataUseCase
import com.tailormyresume.core.domain.account.CollectAccountDataUseCase
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.account.TestAccountDataExporter
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.settings.impl.credits.CreditsScreen
import com.tailormyresume.feature.settings.impl.credits.CreditsUiState
import com.tailormyresume.feature.settings.impl.credits.CreditsViewModel
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountSheetContent
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountUiState
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountViewModel
import com.tailormyresume.feature.settings.impl.paywall.PaywallActions
import com.tailormyresume.feature.settings.impl.paywall.PaywallScreen
import com.tailormyresume.feature.settings.impl.paywall.PaywallUiState
import com.tailormyresume.feature.settings.impl.paywall.PaywallViewModel
import com.tailormyresume.feature.settings.impl.setTmrContent
import com.tailormyresume.feature.settings.impl.settings.SettingsActions
import com.tailormyresume.feature.settings.impl.settings.SettingsScreen
import com.tailormyresume.feature.settings.impl.settings.SettingsUiState
import com.tailormyresume.feature.settings.impl.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class OneBalanceAcrossScreensTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val session = TestSessionRepository()
    private val credits = TestCreditsRepository()
    private val staleGateway = TestPaymentGateway().withFreeCredits(5)
    private val applications = TestApplicationRepository()
    private val settings = TestResumeSettingsRepository()

    private fun entry(kind: CreditLedgerKind, amount: Int, second: Long) =
        CreditLedgerEntry(kind, amount, null, null, Instant.fromEpochSeconds(second))

    private fun settingsViewModel() = SettingsViewModel(
        sessionRepository = session,
        creditBalance = AccountCreditBalance(staleGateway, credits),
        resumeSettingsRepository = settings,
        exportAccountData = ExportAccountDataUseCase(
            collectAccountData = CollectAccountDataUseCase(
                sessionRepository = session,
                profileRepository = TestProfileRepository(),
                applicationRepository = applications,
                exportHistoryRepository = TestExportHistoryRepository(),
                paymentGateway = staleGateway,
                creditsRepository = credits,
                resumeSettingsRepository = settings,
                clock = TestClock(),
            ),
            exporter = TestAccountDataExporter(),
        ),
        signInGateway = TestSignInGateway(session),
    )

    private fun deleteViewModel() = DeleteAccountViewModel(
        DeleteAccountUseCase(
            applicationRepository = applications,
            profileRepository = TestProfileRepository(),
            exportHistoryRepository = TestExportHistoryRepository(),
            sessionRepository = session,
            signInGateway = TestSignInGateway(session),
            serverAccountDeleter = com.tailormyresume.feature.settings.impl.delete.RecordingServerAccountDeleter(),
            creditBalance = AccountCreditBalance(staleGateway, credits),
            latency = NoMockLatency,
            creditsRepository = credits,
            resumeSettingsRepository = settings,
        ),
    )

    private inline fun <reified T> stateOf(crossinline flow: () -> kotlinx.coroutines.flow.Flow<Any>): T =
        runBlocking { flow().first { it is T } as T }

    @Test
    fun settingsCreditsPaywallDeleteSheetAndTheApplicationsSourceShowOneNumber() {
        credits.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 1, 1),
                entry(CreditLedgerKind.PURCHASE, 15, 2),
                entry(CreditLedgerKind.SPEND, -3, 3),
            ),
        )
        runBlocking { applications.upsertApplication(sampleApplication.copy(id = "one")) }
        val settingsState = stateOf<SettingsUiState.Content> { settingsViewModel().uiState }
        val creditsState = stateOf<CreditsUiState.Content> { CreditsViewModel(credits, applications).uiState }
        val paywallState = stateOf<PaywallUiState> { PaywallViewModel(staleGateway, credits, TestClock()).uiState }
        val deleteState = stateOf<DeleteAccountUiState.Content> { deleteViewModel().uiState }
        var shown by mutableIntStateOf(0)
        rule.setTmrContent(TmrTestDevices.prototype) {
            when (shown) {
                0 -> SettingsScreen(settingsState, "1.0", SettingsActions({}, {}, {}, {}, {}, {}, {}, {}))
                1 -> CreditsScreen(creditsState, onBuyMore = {})
                2 -> PaywallScreen(paywallState, PaywallActions({}, {}, {}))
                else -> DeleteAccountSheetContent(deleteState, {}, {}, {})
            }
        }

        rule.onNodeWithText("13 left").assertExists()
        shown = 1
        rule.onAllNodesWithText("13").assertCountAtLeast(1)
        shown = 2
        rule.onNodeWithText("13 credits left").assertExists()
        shown = 3
        rule.onNodeWithText("13 unused credits", substring = true).assertExists()

        assertThat(
            listOf(settingsState.credits, creditsState.credits, paywallState.credits, deleteState.unusedCredits),
        ).containsExactly(13, 13, 13, 13)
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountAtLeast(count: Int) {
    assertThat(fetchSemanticsNodes().size).isAtLeast(count)
}
