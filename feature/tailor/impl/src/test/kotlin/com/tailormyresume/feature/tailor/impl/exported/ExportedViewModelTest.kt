package com.tailormyresume.feature.tailor.impl.exported

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import com.tailormyresume.feature.tailor.impl.acceptedApplication
import com.tailormyresume.feature.tailor.impl.acceptedBullet
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.export.ExportDirectory
import com.tailormyresume.feature.tailor.impl.export.ExportResumeUseCase
import com.tailormyresume.feature.tailor.impl.export.FakeResumePdfRenderer
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class ExportedViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val settingsRepository = TestResumeSettingsRepository()
    private val exportHistory = TestExportHistoryRepository()
    private val creditsRepository = TestCreditsRepository()
    private val paymentGateway = TestPaymentGateway().withFreeCredits(0)
    private val clock = TestClock()

    private val b1 = acceptedBullet(
        id = "b1",
        original = "Built an internal tool",
        proposed = "Developed an internal tool",
    )

    private val initialZone: TimeZone = TimeZone.getDefault()

    private lateinit var directory: File
    private lateinit var renderer: FakeResumePdfRenderer
    private var filesPresent = true
    private val intentRequests = mutableListOf<String>()
    private val exportedPdfIntents = object : ExportedPdfIntents {
        override suspend fun shareIntent(applicationId: String): Intent? {
            intentRequests += "share:$applicationId"
            return if (filesPresent) Intent(Intent.ACTION_SEND) else null
        }

        override suspend fun viewIntent(applicationId: String): Intent? {
            intentRequests += "view:$applicationId"
            return if (filesPresent) Intent(Intent.ACTION_VIEW) else null
        }
    }

    private val fileStore = object : ExportedFileStore {
        override fun fileFor(applicationId: String, fileName: String): File? {
            if (!filesPresent) return null
            return ExportDirectory(directory).existing(applicationId, fileName)
        }
    }

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        directory = folder.newFolder()
        renderer = FakeResumePdfRenderer(directory)
        filesPresent = true
        intentRequests.clear()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(initialZone)
    }

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        creditBalance: AccountCreditBalance = AccountCreditBalance(paymentGateway, creditsRepository),
    ) = ExportedViewModel(
        applicationRepository = applicationRepository,
        creditsRepository = creditsRepository,
        creditBalance = creditBalance,
        exportResume = ExportResumeUseCase(
            applicationRepository,
            profileRepository,
            settingsRepository,
            exportHistory,
            ResumeDocumentAssembler(TestResumeHeadings),
            renderer,
            clock,
        ),
        setApplicationStatus = SetApplicationStatusUseCase(applicationRepository, clock),
        fileStore = fileStore,
        exportedPdfIntents = exportedPdfIntents,
        savedStateHandle = savedStateHandle,
        ioDispatcher = UnconfinedTestDispatcher(),
        applicationId = "app-1",
    )

    private fun seed(application: JobApplication = acceptedApplication(listOf(b1))) {
        applicationRepository.sendApplications(listOf(application))
        profileRepository.sendProfile(
            testProfile(listOf(entryFor("exp-1", b1))).copy(fullName = "Priya Deshmukh"),
        )
    }

    private fun entry(
        kind: CreditLedgerKind,
        amount: Int,
        applicationId: String? = null,
        productId: String? = null,
    ): CreditLedgerEntry = CreditLedgerEntry(
        kind = kind,
        amount = amount,
        applicationId = applicationId,
        productId = productId,
        createdAt = clock.now(),
    )

    private val markedOnText: String
        get() = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
            .withZone(ZoneId.of("UTC"))
            .format(java.time.Instant.ofEpochSecond(clock.now().epochSeconds))

    private fun ExportedViewModel.ready(): ExportedUiState.Ready {
        val current = state.value
        assertThat(current).isInstanceOf(ExportedUiState.Ready::class.java)
        return current as ExportedUiState.Ready
    }

    private suspend fun storedApplication(): JobApplication {
        val stored = applicationRepository.observeApplication("app-1").first()
        assertThat(stored).isNotNull()
        return stored ?: error("application app-1 missing")
    }

    @Test
    fun paidExportShowsCreditsLine() = runTest {
        paymentGateway.withFreeCredits(3)
        creditsRepository.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 1),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "hr"),
                entry(CreditLedgerKind.PURCHASE, 5, productId = "application_pack_5"),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "kb"),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "app-1"),
            ),
        )
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        val ready = viewModel.ready()

        assertThat(ready.creditsLeft).isEqualTo(3)
        assertThat(ready.freeResumeUsed).isFalse()
        assertThat(ready.fileName).isEqualTo("Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf")
        assertThat(ready.pageCount).isEqualTo(1)
        assertThat(ready.sizeKb).isEqualTo(1)
        assertThat(ready.jobTitle).isEqualTo("Associate Analyst")
        assertThat(ready.company).isEqualTo("Northwind GCC")
        assertThat(ready.status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(ready.markedOn).isNull()
        assertThat(ready.statusSheet).isNull()
    }

    @Test
    fun ledgerBalanceUpdatesWhileWalletStaysStale() = runTest {
        paymentGateway.withFreeCredits(3)
        creditsRepository.sendLedger(listOf(entry(CreditLedgerKind.FREE_GRANT, 3)))
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        assertThat(viewModel.ready().creditsLeft).isEqualTo(3)
        creditsRepository.record(entry(CreditLedgerKind.SPEND, -1, applicationId = "another-application"))
        assertThat(viewModel.ready().creditsLeft).isEqualTo(2)
    }

    @Test
    fun unknownWalletIsNotZeroCredits() = runTest {
        val unknown = PurchaseEntitlement(0, 0, emptyList(), hasKnownBalance = false)
        val gateway = object : PaymentGateway by paymentGateway {
            override fun observeEntitlement() = flowOf(unknown)
        }
        seed()
        val unknownCredits = object : CreditsRepository by creditsRepository {
            override fun observeKnownBalance() = flowOf<Int?>(null)
        }
        val viewModel = viewModel(creditBalance = AccountCreditBalance(gateway, unknownCredits))
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        assertThat(viewModel.ready().creditsLeft).isNull()
    }

    @Test
    fun freeExportShowsNothingChargedLine() = runTest {
        creditsRepository.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 1),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "app-1"),
            ),
        )
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        val ready = viewModel.ready()

        assertThat(ready.creditsLeft).isEqualTo(0)
        assertThat(ready.freeResumeUsed).isTrue()
    }

    @Test
    fun reexportOfTheFreeApplicationAfterAPurchaseStillShowsTheFreeLine() = runTest {
        creditsRepository.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 1),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "app-1"),
                entry(CreditLedgerKind.PURCHASE, 5, productId = "application_pack_5"),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "kb"),
            ),
        )
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        assertThat(viewModel.ready().freeResumeUsed).isTrue()
    }

    @Test
    fun anotherApplicationsSpendDoesNotMakeThisOneFree() = runTest {
        paymentGateway.withFreeCredits(2)
        creditsRepository.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 3),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "kb"),
            ),
        )
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        val ready = viewModel.ready()

        assertThat(ready.freeResumeUsed).isFalse()
        assertThat(ready.creditsLeft).isEqualTo(2)
    }

    @Test
    fun aLaterPaidSpendOfTheSameApplicationClearsTheFreeLine() = runTest {
        creditsRepository.sendLedger(
            listOf(
                entry(CreditLedgerKind.FREE_GRANT, 1),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "app-1"),
                entry(CreditLedgerKind.PURCHASE, 5, productId = "pack"),
                entry(CreditLedgerKind.SPEND, -1, applicationId = "app-1"),
            ),
        )
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        assertThat(viewModel.ready().freeResumeUsed).isFalse()
    }

    @Test
    fun restoreAfterProcessDeathShowsTheStoredExportWithoutExportingAgain() = runTest {
        seed()
        val first = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { first.state.collect() }
        val exported = first.ready()
        assertThat(renderer.renderCalls).isEqualTo(1)

        val restored = viewModel(SavedStateHandle(mapOf("exported" to true)))
        backgroundScope.launch(UnconfinedTestDispatcher()) { restored.state.collect() }

        assertThat(restored.ready().fileName).isEqualTo(exported.fileName)
        assertThat(restored.ready().pageCount).isEqualTo(exported.pageCount)
        assertThat(renderer.renderCalls).isEqualTo(1)
        assertThat(exportHistory.observeExports("app-1").first()).hasSize(1)
    }

    @Test
    fun missingStoredFileAfterProcessDeathDoesNotRerenderCurrentProfile() = runTest {
        seed()
        val first = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { first.state.collect() }
        first.ready()
        filesPresent = false

        val restored = viewModel(SavedStateHandle(mapOf("exported" to true)))
        backgroundScope.launch(UnconfinedTestDispatcher()) { restored.state.collect() }

        assertThat(restored.state.value).isEqualTo(ExportedUiState.ExportFailed)
        assertThat(renderer.renderCalls).isEqualTo(1)
        assertThat(exportHistory.observeExports("app-1").first()).hasSize(1)
    }

    @Test
    fun exportingMarksTheSavedStateSoARestoreDoesNotRepeatIt() = runTest {
        seed()
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle)
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        viewModel.ready()

        assertThat(handle.get<Boolean>("exported")).isTrue()
    }

    @Test
    fun markAppliedSetsStatusDateAndUndoRestores() = runTest {
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        viewModel.events.test {
            viewModel.onMarkApplied()

            assertThat(awaitItem()).isEqualTo(ExportedEvent.MarkedApplied(markedOnText))
            cancelAndIgnoreRemainingEvents()
        }

        val applied = storedApplication()
        assertThat(applied.status).isEqualTo(ApplicationStatus.APPLIED)
        assertThat(applied.appliedOn).isEqualTo(clock.now())
        val ready = viewModel.ready()
        assertThat(ready.status).isEqualTo(ApplicationStatus.APPLIED)
        assertThat(ready.markedOn).isEqualTo(markedOnText)

        viewModel.onUndoApplied()

        val undone = storedApplication()
        assertThat(undone.status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(undone.appliedOn).isNull()
        assertThat(viewModel.ready().markedOn).isNull()
    }

    @Test
    fun changeStatusSavesThroughSharedUseCase() = runTest {
        seed(acceptedApplication(listOf(b1), status = ApplicationStatus.APPLIED, appliedOn = clock.now()))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        assertThat(viewModel.ready().markedOn).isEqualTo(markedOnText)

        viewModel.onChangeStatus()
        assertThat(viewModel.ready().statusSheet).isEqualTo(ApplicationStatus.APPLIED)

        viewModel.onPickStatus(ApplicationStatus.INTERVIEW)
        assertThat(viewModel.ready().statusSheet).isEqualTo(ApplicationStatus.INTERVIEW)
        assertThat(storedApplication().status).isEqualTo(ApplicationStatus.APPLIED)

        viewModel.events.test {
            viewModel.onSaveStatus()

            assertThat(awaitItem()).isEqualTo(ExportedEvent.StatusSet(ApplicationStatus.INTERVIEW))
            cancelAndIgnoreRemainingEvents()
        }

        val interview = storedApplication()
        assertThat(interview.status).isEqualTo(ApplicationStatus.INTERVIEW)
        assertThat(viewModel.ready().statusSheet).isNull()

        viewModel.onChangeStatus()
        viewModel.onPickStatus(ApplicationStatus.SAVED)
        viewModel.events.test {
            viewModel.onSaveStatus()

            assertThat(awaitItem()).isEqualTo(ExportedEvent.StatusSet(ApplicationStatus.SAVED))
            cancelAndIgnoreRemainingEvents()
        }

        val saved = storedApplication()
        assertThat(saved.status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(saved.appliedOn).isNull()
        assertThat(viewModel.ready().markedOn).isNull()

        viewModel.onChangeStatus()
        viewModel.onPickStatus(ApplicationStatus.REJECTED)
        viewModel.onDismissStatusSheet()

        assertThat(viewModel.ready().statusSheet).isNull()
        assertThat(storedApplication().status).isEqualTo(ApplicationStatus.SAVED)
    }

    @Test
    fun shareAndOpenEmitIntentEventsForTheFile() = runTest {
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        viewModel.ready()

        viewModel.events.test {
            viewModel.onShare()
            val share = awaitItem()
            assertThat(share).isInstanceOf(ExportedEvent.Share::class.java)
            assertThat((share as ExportedEvent.Share).intent.action).isEqualTo(Intent.ACTION_SEND)

            viewModel.onOpen()
            val open = awaitItem()
            assertThat(open).isInstanceOf(ExportedEvent.Open::class.java)
            assertThat((open as ExportedEvent.Open).intent.action).isEqualTo(Intent.ACTION_VIEW)

            cancelAndIgnoreRemainingEvents()
        }

        filesPresent = false

        viewModel.events.test {
            viewModel.onShare()
            assertThat(awaitItem()).isEqualTo(ExportedEvent.FileMissing)

            viewModel.onOpen()
            assertThat(awaitItem()).isEqualTo(ExportedEvent.FileMissing)

            cancelAndIgnoreRemainingEvents()
        }

        assertThat(intentRequests).containsExactly("share:app-1", "view:app-1", "share:app-1", "view:app-1").inOrder()
    }

    @Test
    fun soonRowsOnlyEmitComingSoonToast() = runTest {
        seed()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        val before = storedApplication()

        viewModel.events.test {
            viewModel.onSoon()

            assertThat(awaitItem()).isEqualTo(ExportedEvent.ComingSoon)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(storedApplication()).isEqualTo(before)
    }

    @Test
    fun deepLinkWithoutAcceptShowsNothingExported() = runTest {
        seed(acceptedApplication(listOf(b1), accepted = false))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        assertThat(viewModel.state.value).isEqualTo(ExportedUiState.NothingExported)
        assertThat(renderer.renderCalls).isEqualTo(0)
        assertThat(storedApplication().exportFileName).isNull()
    }
}
