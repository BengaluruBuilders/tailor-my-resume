package com.tailormyresume.feature.analysis.impl.result

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.domain.coverage.ScreenForKeywords
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.analysis.impl.ResultTestData
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class JobResultViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val paymentGateway = TestPaymentGateway().withFreeCredits(0)
    private val creditsRepository = TestCreditsRepository()

    @Test
    fun readyStateHasComputedCoverageAndKeywordScreen() = runTest {
        val answer = QuickAnswer(ResultTestData.STAKEHOLDER_ID, "YES_REGULARLY", ResultTestData.STAKEHOLDER_DETAIL)
        seed(ResultTestData.application(quickAnswer = answer))
        val state = readyState(viewModel())
        val coverage = KeywordCoverageCalculator.compute(ResultTestData.matches, answer, null, profile = ResultTestData.profile)
        val screen = ScreenForKeywords(ResultTestData.matches, profile = ResultTestData.profile)

        assertThat(state.title).isEqualTo("Associate Analyst")
        assertThat(state.company).isEqualTo("Northwind GCC")
        assertThat(state.location).isEqualTo("Bengaluru · Hybrid")
        assertThat(state.now).isEqualTo(coverage.now)
        assertThat(state.upTo).isEqualTo(coverage.upTo)
        assertThat(state.upTo).isGreaterThan(state.now)
        assertThat(state.have).isEqualTo(screen.have)
        assertThat(state.missing).isEqualTo(screen.missing)
        assertThat(state.keywordCount).isEqualTo(state.have.size + state.missing.size)
        assertThat(state.asksQuestion).isFalse()
    }

    @Test
    fun mustHavesCarryReasonsAndUnclearRowIsTheQuestionRequirement() = runTest {
        seed()
        val state = readyState(viewModel())

        assertThat(state.mustHaves).containsExactly(
            MustHaveRow("r-sql", ResultTestData.sql.text, MatchStatus.MET, "Used SQL daily at your internship", false),
            MustHaveRow("r-bi", ResultTestData.powerBi.text, MatchStatus.PARTIAL, "Power BI at your internship, no Tableau", false),
            MustHaveRow(
                ResultTestData.STAKEHOLDER_ID,
                ResultTestData.stakeholders.text,
                MatchStatus.GAP,
                null,
                true,
            ),
        ).inOrder()
        assertThat(state.mustHaves.map { it.requirementId }).doesNotContain("r-py")
        assertThat(state.mustHaves.count { it.unclear }).isEqualTo(1)
        assertThat(state.asksQuestion).isTrue()
    }

    @Test
    fun positiveStoredAnswerMarksTheQuestionRequirementAsAnswered() = runTest {
        seed(
            ResultTestData.application(
                quickAnswer = QuickAnswer(ResultTestData.STAKEHOLDER_ID, "YES_REGULARLY", ""),
            ),
        )
        val state = readyState(viewModel())

        assertThat(state.mustHaves.count { it.unclear }).isEqualTo(0)
        val answered = state.mustHaves.first { it.requirementId == ResultTestData.STAKEHOLDER_ID }
        assertThat(answered.status).isEqualTo(MatchStatus.GAP)
        assertThat(answered.answered).isTrue()
        assertThat(answered.marker).isEqualTo(MustHaveMarkerKind.ANSWERED)
        assertThat(state.asksQuestion).isFalse()
    }

    @Test
    fun nonGapReasonWithoutCitedEvidenceRemainsVisible() = runTest {
        val matches = listOf(
            RequirementMatch(ResultTestData.sql, MatchStatus.MET, emptyList(), "Used SQL daily at your internship"),
            RequirementMatch(ResultTestData.powerBi, MatchStatus.GAP, emptyList(), "No BI tool on the resume"),
        )
        seed(ResultTestData.application(matches = matches, question = null))
        val rows = readyState(viewModel()).mustHaves

        assertThat(rows.map { it.requirementId }).containsExactly("r-sql", "r-bi").inOrder()
        assertThat(rows[0].reason).isEqualTo("Used SQL daily at your internship")
        assertThat(rows[1].reason).isEqualTo("No BI tool on the resume")
    }

    @Test
    fun gapWithReasonShowsGapMarkerNotCheck() = runTest {
        val matches = listOf(
            RequirementMatch(ResultTestData.sql, MatchStatus.MET, listOf("e1"), "Used SQL daily"),
            RequirementMatch(ResultTestData.powerBi, MatchStatus.PARTIAL, listOf("e2"), "No Tableau"),
            RequirementMatch(ResultTestData.stakeholders, MatchStatus.GAP, emptyList(), "No stakeholder work on the resume"),
        )
        seed(ResultTestData.application(matches = matches, question = null))
        val rows = readyState(viewModel()).mustHaves

        assertThat(rows.map { it.marker }).containsExactly(
            MustHaveMarkerKind.MET,
            MustHaveMarkerKind.PARTIAL,
            MustHaveMarkerKind.GAP,
        ).inOrder()
        assertThat(rows[2].reason).isNotNull()
    }

    @Test
    fun balanceDrivesCreditNote() = runTest {
        seed(credits = 2)
        val viewModel = viewModel()
        assertThat(readyState(viewModel).credits).isEqualTo(2)

        creditsRepository.record(CreditLedgerEntry(CreditLedgerKind.SPEND, -1, ResultTestData.APP_ID, null, Instant.parse("2026-10-12T09:01:00Z")))
        creditsRepository.record(CreditLedgerEntry(CreditLedgerKind.SPEND, -1, ResultTestData.APP_ID, null, Instant.parse("2026-10-12T09:02:00Z")))

        assertThat(readyState(viewModel).credits).isEqualTo(0)
    }

    @Test
    fun ledgerBalanceDrivesTheDisplayedCountAndTailoringGate() = runTest {
        seed(credits = 0)
        val ledger = TestCreditsRepository()
        ledger.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-12T09:00:00Z")))
        val viewModel = viewModel(balance = AccountCreditBalance(paymentGateway, ledger))

        assertThat(readyState(viewModel).credits).isEqualTo(1)
        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.QuickQuestion(ResultTestData.APP_ID))
            ledger.record(CreditLedgerEntry(CreditLedgerKind.SPEND, -1, ResultTestData.APP_ID, null, Instant.parse("2026-10-12T09:01:00Z")))
            viewModel.onShown()
            assertThat(readyState(viewModel).credits).isEqualTo(0)
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.Paywall(ResultTestData.APP_ID))
        }
    }

    @Test
    fun locationFallsBackToTheJobWhenApplicationLocationIsBlank() = runTest {
        seed(ResultTestData.application(location = "   "))

        assertThat(readyState(viewModel()).location).isEqualTo("Pune")
    }

    @Test
    fun unavailableWhenApplicationIsMissing() = runTest {
        seed(application = null)
        assertThat(viewModel().uiState.value).isEqualTo(JobResultUiState.Unavailable)

        seed(ResultTestData.application().copy(gapAnalysis = null))
        assertThat(viewModel().uiState.value).isEqualTo(JobResultUiState.Unavailable)
    }

    @Test
    fun tailorRoutesToPaywallQuestionOrTailoring() = runTest {
        val answered = ResultTestData.application(
            quickAnswer = QuickAnswer(ResultTestData.STAKEHOLDER_ID, "A_FEW_TIMES", ResultTestData.STAKEHOLDER_DETAIL),
        )

        assertTailorEvent(ResultTestData.application(), credits = 0, JobResultEvent.Paywall(ResultTestData.APP_ID))
        assertTailorEvent(ResultTestData.application(), credits = 1, JobResultEvent.QuickQuestion(ResultTestData.APP_ID))
        assertTailorEvent(
            ResultTestData.application(question = null),
            credits = 1,
            JobResultEvent.Tailor(ResultTestData.APP_ID),
        )
        assertTailorEvent(answered, credits = 1, JobResultEvent.Tailor(ResultTestData.APP_ID))
    }

    @Test
    fun unknownBalanceDoesNotPromptAPurchase() = runTest {
        seed()
        val gateway = object : PaymentGateway by paymentGateway {
            override fun observeEntitlement() = flowOf(PurchaseEntitlement(0, 0, emptyList(), hasKnownBalance = false))
        }
        val unknownCredits = object : CreditsRepository by creditsRepository {
            override fun observeKnownBalance() = flowOf<Int?>(null)
        }
        val viewModel = viewModel(gateway, AccountCreditBalance(gateway, unknownCredits))
        assertThat(readyState(viewModel).credits).isNull()

        viewModel.events.test {
            viewModel.onTailor()
            expectNoEvents()
        }
    }

    @Test
    fun onTailorBeforeReadyEmitsNothing() = runTest {
        seed(application = null, credits = 0)
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value).isEqualTo(JobResultUiState.Unavailable)

        viewModel.events.test {
            viewModel.onTailor()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun doubleTapOnTailorEmitsOneEvent() = runTest {
        seed(ResultTestData.application(), credits = 1)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onTailor()
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.QuickQuestion(ResultTestData.APP_ID))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tailorWorksAgainAfterTheScreenIsShownAgain() = runTest {
        seed(ResultTestData.application(), credits = 1)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.QuickQuestion(ResultTestData.APP_ID))
            viewModel.onShown()
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(JobResultEvent.QuickQuestion(ResultTestData.APP_ID))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun assertTailorEvent(
        application: JobApplication,
        credits: Int,
        expected: JobResultEvent,
    ) = runTest {
        seed(application, credits)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onTailor()
            assertThat(awaitItem()).isEqualTo(expected)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun seed(
        application: JobApplication? = ResultTestData.application(),
        credits: Int = 1,
    ) {
        applicationRepository.sendApplications(listOfNotNull(application))
        paymentGateway.withFreeCredits(credits)
        creditsRepository.sendLedger(
            if (credits > 0) listOf(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, credits, null, null, Instant.parse("2026-10-12T09:00:00Z"))) else emptyList(),
        )
    }

    private fun TestScope.viewModel(gateway: PaymentGateway = paymentGateway, balance: AccountCreditBalance = AccountCreditBalance(gateway, creditsRepository)) =
        JobResultViewModel(applicationRepository, balance, TestProfileRepository().apply { sendProfile(ResultTestData.profile) }, ResultTestData.APP_ID).also {
            backgroundScope.launch(UnconfinedTestDispatcher()) { it.uiState.collect() }
        }

    private fun readyState(viewModel: JobResultViewModel): JobResultUiState.Ready {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(JobResultUiState.Ready::class.java)
        return state as JobResultUiState.Ready
    }
}
