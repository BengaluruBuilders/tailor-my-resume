package com.tailormyresume.feature.applications.impl.detail

import android.content.Intent
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.QuickQuestion
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.applications.impl.grant
import com.tailormyresume.feature.applications.impl.prototypeApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.TimeZone

class ApplicationDetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val creditsRepository = TestCreditsRepository()
    private val paymentGateway = TestPaymentGateway().withFreeCredits(0)
    private val creditBalance = AccountCreditBalance(paymentGateway)
    private val intents = FakeExportedPdfIntents()
    private val clock = TestClock()
    private val initialZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(initialZone)
    }

    private fun viewModel(applicationId: String = "kb", balance: AccountCreditBalance = creditBalance) = ApplicationDetailViewModel(
        applicationRepository = applicationRepository,
        creditBalance = balance,
        setApplicationStatus = SetApplicationStatusUseCase(applicationRepository, clock),
        exportedPdfIntents = intents,
        applicationId = applicationId,
    )

    private fun seed(vararg applications: JobApplication) {
        applicationRepository.sendApplications(applications.toList())
    }

    private suspend fun ApplicationDetailViewModel.ready(): ApplicationDetailUiState.Ready =
        state.first { it !is ApplicationDetailUiState.Loading } as ApplicationDetailUiState.Ready

    private suspend fun stored(id: String = "kb"): JobApplication =
        checkNotNull(applicationRepository.observeApplication(id).first())

    private val questionAnalysis = GapAnalysis(
        matches = emptyList(),
        keywordCoverage = KeywordCoverage(covered = 4, total = 8),
        question = QuickQuestion(requirementId = "r1", text = "Have you presented to senior leaders?", why = "The job asks for it."),
    )

    @Test
    fun readyShowsRoleCompanyPlaceStatusAndFile() = runTest {
        seed(prototypeApplication("kb"))

        val ready = viewModel().ready()

        assertThat(ready).isEqualTo(
            ApplicationDetailUiState.Ready(
                title = "Business Analyst",
                company = "Kestrel Bank",
                place = "Mumbai · On-site",
                status = ApplicationStatus.INTERVIEW,
                appliedOn = "6 Oct",
                file = ApplicationFile("Priya-Deshmukh_Kestrel-Bank_Business-Analyst.pdf", keywordPercent = 88),
                statusSheet = null,
            ),
        )
    }

    @Test
    fun savedShowsNotAppliedYet() = runTest {
        seed(prototypeApplication("hr"))

        val ready = viewModel("hr").ready()

        assertThat(ready.status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(ready.appliedOn).isNull()
    }

    @Test
    fun noExportFileHidesFileCard() = runTest {
        seed(prototypeApplication("kb").copy(exportFileName = null))

        assertThat(viewModel().ready().file).isNull()
    }

    @Test
    fun missingApplicationIsNotFound() = runTest {
        seed(prototypeApplication("kb"))

        val state = viewModel("gone").state.first { it !is ApplicationDetailUiState.Loading }

        assertThat(state).isEqualTo(ApplicationDetailUiState.NotFound)
    }

    @Test
    fun changeOpensSheetPreselectedAndSaveStoresThroughSharedUseCase() = runTest {
        seed(prototypeApplication("kb"))
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onChangeStatus()
            assertThat(viewModel.ready().statusSheet).isEqualTo(ApplicationStatus.INTERVIEW)

            viewModel.onPickStatus(ApplicationStatus.OFFER)
            assertThat(viewModel.ready().statusSheet).isEqualTo(ApplicationStatus.OFFER)
            assertThat(stored().status).isEqualTo(ApplicationStatus.INTERVIEW)

            viewModel.onSaveStatus()

            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.StatusSet(ApplicationStatus.OFFER))
            assertThat(stored().status).isEqualTo(ApplicationStatus.OFFER)
            assertThat(viewModel.ready().statusSheet).isNull()
        }
    }

    @Test
    fun savedStatusClearsAppliedDate() = runTest {
        seed(prototypeApplication("kb"))
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.onChangeStatus()
        viewModel.onPickStatus(ApplicationStatus.SAVED)
        viewModel.onSaveStatus()

        assertThat(stored().status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(stored().appliedOn).isNull()
        assertThat(viewModel.ready().appliedOn).isNull()
    }

    @Test
    fun dismissLeavesStatusUntouched() = runTest {
        seed(prototypeApplication("kb"))
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.onChangeStatus()
        viewModel.onPickStatus(ApplicationStatus.REJECTED)
        viewModel.onDismissStatusSheet()

        assertThat(viewModel.ready().statusSheet).isNull()
        assertThat(stored()).isEqualTo(prototypeApplication("kb"))
    }

    @Test
    fun retailorWithCreditsOpensQuickQuestionWhenAnalysisHasQuestion() = runTest {
        seed(prototypeApplication("kb").copy(gapAnalysis = questionAnalysis))
        creditsRepository.sendLedger(listOf(grant(2)))
        paymentGateway.withFreeCredits(2)
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onRetailor()

            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.OpenQuickQuestion("kb"))
        }
    }

    @Test
    fun retailorWithCreditsStartsTailoringWhenNoQuestion() = runTest {
        seed(prototypeApplication("kb").copy(gapAnalysis = questionAnalysis.copy(question = null)))
        creditsRepository.sendLedger(listOf(grant(1)))
        paymentGateway.withFreeCredits(1)
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onRetailor()

            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.StartTailoring("kb"))
        }
    }

    @Test
    fun retailorWithZeroCreditsOpensPaywallForThisApplication() = runTest {
        seed(prototypeApplication("kb").copy(gapAnalysis = questionAnalysis))
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onRetailor()

            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.OpenPaywall("kb"))
        }
    }

    @Test
    fun retailorWithUnknownWalletShowsUnavailableWithoutPaywall() = runTest {
        seed(prototypeApplication("kb"))
        val gateway = object : PaymentGateway by paymentGateway {
            override fun observeEntitlement() = flowOf(PurchaseEntitlement(0, 0, emptyList(), hasKnownBalance = false))
        }
        val viewModel = viewModel(balance = AccountCreditBalance(gateway))
        viewModel.ready()

        viewModel.events.test {
            viewModel.onRetailor()
            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.BalanceUnavailable)
            expectNoEvents()
        }
    }

    @Test
    fun retailorSpendsNoCredit() = runTest {
        seed(prototypeApplication("kb"))
        creditsRepository.sendLedger(listOf(grant(3)))
        paymentGateway.withFreeCredits(3)
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onRetailor()
            awaitItem()
        }

        assertThat(creditsRepository.observeLedger().first()).containsExactly(grant(3))
        assertThat(creditsRepository.observeBalance().first()).isEqualTo(3)
        assertThat(paymentGateway.entitlement().totalCredits).isEqualTo(3)
    }

    @Test
    fun soonRowsOnlyEmitComingSoon() = runTest {
        seed(prototypeApplication("kb"))
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onSoon()

            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.ComingSoon)
            expectNoEvents()
        }
        assertThat(stored()).isEqualTo(prototypeApplication("kb"))
    }

    @Test
    fun tapsWhileAFileRequestRunsAreIgnored() = runTest {
        seed(prototypeApplication("kb"))
        intents.share = Intent("share")
        intents.view = Intent("view")
        intents.latencyMs = 50L
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onShare()
            viewModel.onShare()
            viewModel.onOpen()
            testScheduler.advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(ApplicationDetailEvent.ShareFile::class.java)
            expectNoEvents()
        }
        assertThat(intents.shareRequests).containsExactly("kb")
        assertThat(intents.viewRequests).isEmpty()
    }

    @Test
    fun shareAndOpenEmitIntentEvents() = runTest {
        seed(prototypeApplication("kb"))
        val share = Intent("share")
        val view = Intent("view")
        intents.share = share
        intents.view = view
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onShare()
            val shared = awaitItem() as ApplicationDetailEvent.ShareFile
            viewModel.onOpen()
            val opened = awaitItem() as ApplicationDetailEvent.OpenFile

            assertThat(shared.intent).isSameInstanceAs(share)
            assertThat(opened.intent).isSameInstanceAs(view)
        }
        assertThat(intents.shareRequests).containsExactly("kb")
        assertThat(intents.viewRequests).containsExactly("kb")
    }

    @Test
    fun nullIntentEmitsFileMissing() = runTest {
        seed(prototypeApplication("kb"))
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.events.test {
            viewModel.onShare()
            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.FileMissing)
            viewModel.onOpen()
            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.FileMissing)
        }
    }

    @Test
    fun exposesNoDeleteAction() {
        val names = ApplicationDetailViewModel::class.java.methods.map { it.name } +
            ApplicationDetailEvent::class.java.declaredClasses.map { it.simpleName } +
            ApplicationDetailUiState::class.java.declaredClasses.map { it.simpleName }

        assertThat(names.filter { it.contains("delete", ignoreCase = true) }).isEmpty()
        assertThat(names).contains("onRetailor")
    }
}
