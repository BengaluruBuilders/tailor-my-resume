package com.tailormyresume.feature.settings.impl.credits

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.gateway.TestAccountWallet
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class CreditsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val applications = TestApplicationRepository()
    private val ledger = TestCreditsRepository()
    private val payment = TestAccountWallet().withFreeCredits(3)

    private fun viewModel(credits: com.tailormyresume.core.data.repository.CreditsRepository = ledger) =
        CreditsViewModel(credits, AccountCreditBalance(payment, credits), applications)

    private fun TestScope.collect(viewModel: CreditsViewModel): List<CreditsUiState> {
        val seen = mutableListOf<CreditsUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { seen += it } }
        return seen
    }

    private fun seedReturning() {
        applications.sendApplications(returningScenario.applications)
        ledger.sendLedger(returningScenario.ledger)
    }

    private fun CreditsViewModel.content() = uiState.value as CreditsUiState.Content

    @Test
    fun returningLedgerJoinsApplicationsNewestFirst() = runTest {
        seedReturning()
        val viewModel = viewModel()
        collect(viewModel)

        val content = viewModel.content()

        assertThat(content.credits).isEqualTo(3)
        assertThat(content.history.map { it.title }).containsExactly(
            HistoryTitle.Tailored("Associate Analyst"),
            HistoryTitle.Tailored("Business Analyst"),
            HistoryTitle.BoughtCredits(5),
            HistoryTitle.Tailored("Data Analyst"),
            HistoryTitle.FreeFirstResume,
        ).inOrder()
        assertThat(content.history[2].detail).isEqualTo(HistoryDetail.None)
    }

    @Test
    fun goneApplicationGetsGenericLabel() = runTest {
        ledger.sendLedger(returningScenario.ledger)
        val viewModel = viewModel()
        collect(viewModel)

        val spends = viewModel.content().history.filter { it.amount < 0 }

        assertThat(spends.map { it.title }).containsExactly(
            HistoryTitle.TailoredResume,
            HistoryTitle.TailoredResume,
            HistoryTitle.TailoredResume,
        )
    }

    @Test
    fun deletingAnApplicationRelabelsItsSpend() = runTest {
        seedReturning()
        val viewModel = viewModel()
        collect(viewModel)

        applications.sendApplications(returningScenario.applications.filterNot { it.id == "nw" })

        assertThat(viewModel.content().history.first().title).isEqualTo(HistoryTitle.TailoredResume)
    }

    @Test
    fun balanceComesFromCreditsRepository() = runTest {
        val credits = RecordingCreditsRepository()
        val viewModel = viewModel(credits)
        collect(viewModel)

        assertThat(viewModel.content().credits).isEqualTo(0)
        assertThat(credits.calls).containsExactly("refresh")
    }

    @Test
    fun hundredEntriesJoinOncePerApplicationsChange() = runTest {
        val entries = (1..100).map { day ->
            CreditLedgerEntry(CreditLedgerKind.SPEND, -1, "nw", null, Instant.fromEpochSeconds(1_790_000_000L + day * 60))
        }
        ledger.sendLedger(entries)
        applications.sendApplications(returningScenario.applications)
        val viewModel = viewModel()
        val seen = collect(viewModel)
        val settled = seen.size

        applications.sendApplications(emptyList())

        assertThat(seen.size).isEqualTo(settled + 1)
        assertThat(viewModel.content().history).hasSize(100)
        assertThat(viewModel.content().history.map { it.key }.toSet()).hasSize(100)
    }
}
