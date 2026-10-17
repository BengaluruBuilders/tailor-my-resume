package com.tailormyresume.feature.applications.impl.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.applications.impl.grant
import com.tailormyresume.feature.applications.impl.prototypeApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RetailorUnknownBalanceTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val credits = TestCreditsRepository()

    private fun viewModel() = ApplicationDetailViewModel(
        applicationRepository = applicationRepository,
        creditsRepository = credits,
        setApplicationStatus = SetApplicationStatusUseCase(applicationRepository, TestClock()),
        exportedPdfIntents = FakeExportedPdfIntents(),
        applicationId = "kb",
    )

    private suspend fun retailorEvent(): ApplicationDetailEvent {
        applicationRepository.sendApplications(
            listOf(prototypeApplication("kb").copy(gapAnalysis = GapAnalysis(emptyList(), KeywordCoverage(1, 2), null))),
        )
        val viewModel = viewModel()
        viewModel.state.first { it !is ApplicationDetailUiState.Loading }
        var event: ApplicationDetailEvent? = null
        viewModel.events.test {
            viewModel.onRetailor()
            event = awaitItem()
        }
        return checkNotNull(event)
    }

    @Test
    fun anUnknownBalanceRefreshesBeforeDeciding_andAPayingUserIsNotSentToThePaywall() = runTest {
        credits.sendUnknown()
        credits.revealOnRefresh(listOf(grant(2)))

        assertThat(retailorEvent()).isEqualTo(ApplicationDetailEvent.StartTailoring("kb"))
        assertThat(credits.refreshCalls).isEqualTo(1)
    }

    @Test
    fun aRefreshThatShowsZeroOpensThePaywall() = runTest {
        credits.sendUnknown()
        credits.revealOnRefresh(emptyList())

        assertThat(retailorEvent()).isEqualTo(ApplicationDetailEvent.OpenPaywall("kb"))
    }

    @Test
    fun aBalanceThatStaysUnknownToastsInsteadOfTailoringOrOpeningThePaywall() = runTest {
        credits.sendUnknown()

        assertThat(retailorEvent()).isEqualTo(ApplicationDetailEvent.CreditCheckFailed)
    }

    @Test
    fun aKnownBalanceDoesNotRefresh() = runTest {
        credits.sendLedger(listOf(grant(1)))

        assertThat(retailorEvent()).isEqualTo(ApplicationDetailEvent.StartTailoring("kb"))
        assertThat(credits.refreshCalls).isEqualTo(0)
    }
}
