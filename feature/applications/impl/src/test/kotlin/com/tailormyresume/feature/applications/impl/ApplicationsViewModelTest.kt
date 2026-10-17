package com.tailormyresume.feature.applications.impl

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.gateway.TestAccountWallet
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ApplicationsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val creditsRepository = TestCreditsRepository()
    private val paymentGateway = TestAccountWallet().withFreeCredits(3)
    private val creditBalance = AccountCreditBalance(paymentGateway, creditsRepository)
    private val profileRepository = TestProfileRepository()
    private val scenario = PrototypeFixtures.returning()

    private fun viewModel() = ApplicationsViewModel(applicationRepository, creditBalance, profileRepository)

    private suspend fun ApplicationsViewModel.settled(): ApplicationsUiState =
        state.first { it !is ApplicationsUiState.Loading }

    private suspend fun populated(): ApplicationsUiState.Populated {
        profileRepository.sendProfile(scenario.profile)
        applicationRepository.sendApplications(scenario.applications.map { it.copy(tailoredResume = TailoredResume(emptyList())) })
        creditsRepository.sendLedger(scenario.ledger)
        return viewModel().settled() as ApplicationsUiState.Populated
    }

    @Test
    fun noApplicationsEmitsEmptyWithCreditsAndInitials() = runTest {
        profileRepository.sendProfile(scenario.profile)
        creditsRepository.sendLedger(listOf(grant(1)))
        paymentGateway.withFreeCredits(1)

        val state = viewModel().settled()

        assertThat(state).isEqualTo(ApplicationsUiState.Empty(ApplicationsHeader(credits = 1, initials = "PD")))
    }

    @Test
    fun listCountsStatusesSortsRecentAndLabelsKeywords() = runTest {
        val state = populated()

        assertThat(state.total).isEqualTo(3)
        assertThat(state.header).isEqualTo(ApplicationsHeader(credits = 3, initials = "PD"))
        assertThat(state.statusCounts).containsExactly(
            TmrApplicationStatus.Saved,
            1,
            TmrApplicationStatus.Applied,
            1,
            TmrApplicationStatus.Interview,
            1,
        )
        assertThat(state.rows.map { it.id }).containsExactly("nw", "kb", "hr").inOrder()
        val first = state.rows.first()
        assertThat(first.title).isEqualTo("Associate Analyst")
        assertThat(first.company).isEqualTo("Northwind GCC")
        assertThat(first.keywordPercent).isEqualTo(92)
        assertThat(first.initial).isEqualTo("N")
        assertThat(first.status).isEqualTo(TmrApplicationStatus.Applied)
    }

    @Test
    fun summaryCountsOnlyTailoredApplicationsWhileRowsKeepDrafts() = runTest {
        val tailored = prototypeApplication("nw").copy(tailoredResume = TailoredResume(emptyList()))
        val draft = prototypeApplication("kb").copy(tailoredResume = null)
        applicationRepository.sendApplications(listOf(tailored, draft))
        profileRepository.sendProfile(scenario.profile)

        val state = viewModel().settled() as ApplicationsUiState.Populated

        assertThat(state.total).isEqualTo(1)
        assertThat(state.statusCounts).containsExactly(TmrApplicationStatus.Applied, 1)
        assertThat(state.rows.map { it.id }).containsExactly("nw", "kb").inOrder()
    }

    @Test
    fun discColourIsStablePerIdAndWithinThePalette() = runTest {
        val first = populated().rows
        val second = viewModel().settled().let { (it as ApplicationsUiState.Populated).rows }

        assertThat(first.map { it.colorIndex }).isEqualTo(second.map { it.colorIndex })
        assertThat(first.map { it.colorIndex }).isNotEmpty()
        first.forEach { assertThat(it.colorIndex).isIn(0 until DISC_COLOR_COUNT) }
    }

    @Test
    fun rowMetaUsesFinalCoverageElseUpToElseCompanyOnly() = runTest {
        val base = prototypeApplication("nw")
        applicationRepository.sendApplications(
            listOf(
                base.copy(id = "final", keywordCoverage = ApplicationKeywordCoverage(now = 40, upTo = 90, final = 70)),
                base.copy(id = "upto", keywordCoverage = ApplicationKeywordCoverage(now = 40, upTo = 90, final = null)),
                base.copy(id = "none", keywordCoverage = null),
            ),
        )
        profileRepository.sendProfile(scenario.profile)

        val rows = (viewModel().settled() as ApplicationsUiState.Populated).rows.associateBy { it.id }

        assertThat(rows.getValue("final").keywordPercent).isEqualTo(70)
        assertThat(rows.getValue("upto").keywordPercent).isEqualTo(90)
        assertThat(rows.getValue("none").keywordPercent).isNull()
        assertThat(rows.getValue("none").company).isEqualTo("Northwind GCC")
    }
}
