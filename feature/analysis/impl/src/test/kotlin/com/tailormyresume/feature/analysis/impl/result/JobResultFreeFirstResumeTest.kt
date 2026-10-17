package com.tailormyresume.feature.analysis.impl.result

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.analysis.impl.ResultTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JobResultFreeFirstResumeTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val applications = TestApplicationRepository()
    private val credits = TestCreditsRepository()

    private fun welcomeGrant() =
        CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.fromEpochSeconds(1))

    private fun viewModel(): JobResultViewModel {
        applications.sendApplications(listOf(ResultTestData.application(question = null)))
        return JobResultViewModel(applications, credits, ResultTestData.APP_ID)
    }

    private fun readyState(viewModel: JobResultViewModel) = runBlocking {
        viewModel.uiState.first { it is JobResultUiState.Ready } as JobResultUiState.Ready
    }

    private fun show(state: JobResultUiState.Ready) {
        rule.setContent { TmrTheme { JobResultScreen(state, onTailor = {}) } }
    }

    @Test
    fun aNewUserWithTheWelcomeGrantSeesOneCreditAndTailorsWithoutThePaywall() = runBlocking<Unit> {
        credits.sendLedger(listOf(welcomeGrant()))
        val viewModel = viewModel()
        val state = readyState(viewModel)

        show(state)

        assertThat(state.credits).isEqualTo(1)
        rule.onNodeWithText("Uses 1 credit · you have 1").assertExists()
        rule.onNodeWithText("No credits left · get more to continue").assertDoesNotExist()
        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.Tailor(ResultTestData.APP_ID))
        }
    }

    @Test
    fun anUnknownBalanceNeverSaysNoCreditsLeft() = runBlocking<Unit> {
        credits.sendUnknown()
        val state = readyState(viewModel())

        show(state)

        assertThat(state.credits).isNull()
        rule.onNodeWithText("No credits left · get more to continue").assertDoesNotExist()
        rule.onNodeWithText("Uses 1 credit").assertExists()
    }

    @Test
    fun tailorOnAnUnknownBalanceRefreshesFirstAndAPayingUserSkipsThePaywall() = runBlocking<Unit> {
        credits.sendUnknown()
        credits.revealOnRefresh(listOf(welcomeGrant().copy(amount = 4)))
        val viewModel = viewModel()
        readyState(viewModel)

        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.Tailor(ResultTestData.APP_ID))
        }
        assertThat(credits.refreshCalls).isEqualTo(1)
    }

    @Test
    fun aRefreshThatShowsZeroOpensThePaywall() = runBlocking<Unit> {
        credits.sendUnknown()
        credits.revealOnRefresh(emptyList())
        val viewModel = viewModel()
        readyState(viewModel)

        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.Paywall(ResultTestData.APP_ID))
        }
    }

    @Test
    fun aBalanceThatStaysUnknownToastsAndStaysSoTailorWorksAgain() = runBlocking<Unit> {
        credits.sendUnknown()
        val viewModel = viewModel()
        readyState(viewModel)

        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.CreditCheckFailed)
            credits.revealOnRefresh(listOf(welcomeGrant()))
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.Tailor(ResultTestData.APP_ID))
        }
    }
}
