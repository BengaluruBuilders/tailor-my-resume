package com.tailormyresume.feature.applications.impl

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OneBalanceApplicationsTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val credits = TestCreditsRepository()

    private fun entry(kind: CreditLedgerKind, amount: Int, second: Long) =
        CreditLedgerEntry(kind, amount, null, null, Instant.fromEpochSeconds(second))

    private fun settledHeader(): ApplicationsHeader = runBlocking {
        val state = ApplicationsViewModel(TestApplicationRepository(), credits, TestProfileRepository())
            .state.first { it !is ApplicationsUiState.Loading }
        (state as ApplicationsUiState.Empty).header
    }

    private fun show(header: ApplicationsHeader) {
        rule.setContent {
            TmrTheme {
                ApplicationsScreen(ApplicationsUiState.Empty(header), onCredits = {}, onAvatar = {}, onAddJob = {}, onOpenApplication = {})
            }
        }
    }

    @Test
    fun theCreditPillShowsTheSharedBalance() {
        credits.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 1, 1),
                entry(CreditLedgerKind.PURCHASE, 15, 2),
                entry(CreditLedgerKind.SPEND, -3, 3),
            ),
        )

        val header = settledHeader()
        show(header)

        assertThat(header.credits).isEqualTo(13)
        rule.onNodeWithText("13 credits").assertExists()
    }

    @Test
    fun anUnknownBalanceShowsADashNotZeroCredits() {
        credits.sendUnknown()

        val header = settledHeader()
        show(header)

        assertThat(header.credits).isNull()
        rule.onNodeWithText("—").assertExists()
        rule.onNodeWithText("0 credits").assertDoesNotExist()
    }
}
