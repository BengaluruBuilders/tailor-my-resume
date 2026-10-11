package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.LegacyDataPurge
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.domain.DiscardJobDraftsUseCase
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.CreditKind
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.canonicalApplication
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class DeleteMyDataUseCaseTest {

    private val applications = TestApplicationRepository().apply {
        sendApplications(listOf(canonicalApplication, canonicalApplication.copy(id = "application-second")))
    }
    private val profiles = TestProfileRepository().apply { sendProfile(canonicalCandidateProfile) }
    private val exportHistory = TestExportHistoryRepository().apply {
        sendExports(
            listOf(
                ExportRecord(
                    applicationId = canonicalApplication.id,
                    format = ExportFormat.PDF,
                    fileName = "resume.pdf",
                    exportedAt = Instant.fromEpochSeconds(1),
                    creditKind = CreditKind.FREE,
                ),
            ),
        )
    }
    private val session = TestSessionRepository()
    private val credits = TestCreditsRepository()
    private val settings = TestResumeSettingsRepository()
    private val contentReports = TestContentReportRepository()
    private val store = TestMockStateStore()
    private var exportedFilesDeletions = 0
    private var transientClears = 0
    private var failingApplicationIds: Set<String> = emptySet()
    private var failingExportedFiles = false

    private val failingApplications = object : ApplicationRepository by applications {
        override suspend fun deleteApplication(id: String) {
            if (id in failingApplicationIds) throw IllegalStateException("disk error")
            applications.deleteApplication(id)
        }
    }

    private fun useCase() = DeleteMyDataUseCase(
        applicationRepository = failingApplications,
        profileRepository = profiles,
        exportHistoryRepository = exportHistory,
        sessionRepository = session,
        discardJobDrafts = DiscardJobDraftsUseCase(contentReports),
        exportedFiles = ExportedFiles {
            if (failingExportedFiles) throw IllegalStateException("cache locked")
            exportedFilesDeletions++
        },
        transientData = TransientDataCleaner { transientClears++ },
        legacyDataPurge = LegacyDataPurge(store),
        creditsRepository = credits,
        resumeSettingsRepository = settings,
    )

    private suspend fun keepAccountState() {
        session.saveAccount(SignInAccount.localAccount)
        session.markOnboardingComplete()
        session.keepJobDescription(KeptJobDescription(text = "jd", company = "Acme", role = "Analyst"))
    }

    @Test
    fun deletesTheProfileApplicationsExportsKeptJobAndFiles() = runTest {
        keepAccountState()

        val result = useCase()()

        assertThat(result.isSuccess).isTrue()
        assertThat(profiles.observeProfile().first()).isNull()
        assertThat(applications.observeApplications().first()).isEmpty()
        assertThat(exportHistory.observeExports().first()).isEmpty()
        assertThat(session.observeKeptJobDescription().first()).isNull()
        assertThat(exportedFilesDeletions).isEqualTo(1)
        assertThat(transientClears).isEqualTo(1)
    }

    @Test
    fun clearsTheResumeSettingsAndTheLedgerRowsOfDeletedApplications() = runTest {
        credits.record(CreditLedgerEntry(CreditLedgerKind.SPEND, -1, canonicalApplication.id, null, Instant.fromEpochSeconds(2)))
        credits.record(CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, null, "application_pack_5", Instant.fromEpochSeconds(1)))
        settings.update { it.copy(pageSize = PageSize.LETTER, productUpdates = true) }

        useCase()()

        assertThat(credits.observeLedger().first().map { it.applicationId }).containsExactly(null)
        assertThat(settings.observeSettings().first()).isEqualTo(ResumeSettings())
    }

    @Test
    fun keepsTheAccountAndOnboardingFlag() = runTest {
        keepAccountState()

        useCase()()

        assertThat(session.observeAccount().first()).isEqualTo(SignInAccount.localAccount)
        assertThat(session.observeOnboardingComplete().first()).isTrue()
    }

    @Test
    fun whenAnApplicationFailsToDelete_reportsTheFailureAndKeepsTheRest() = runTest {
        failingApplicationIds = setOf("application-second")

        val result = useCase()()

        assertThat(result.isFailure).isTrue()
        assertThat(applications.observeApplications().first().map { it.id }).containsExactly("application-second")
    }

    @Test
    fun retryingAfterAFailureFinishesTheDeletion() = runTest {
        failingApplicationIds = setOf("application-second")
        useCase()()
        failingApplicationIds = emptySet()

        val retry = useCase()()

        assertThat(retry.isSuccess).isTrue()
        assertThat(applications.observeApplications().first()).isEmpty()
        assertThat(profiles.observeProfile().first()).isNull()
        assertThat(exportedFilesDeletions).isEqualTo(1)
    }

    @Test
    fun whenTheExportedFilesFail_reportsTheFailureAndRetryCompletes() = runTest {
        failingExportedFiles = true

        val failed = useCase()()
        failingExportedFiles = false
        val retry = useCase()()

        assertThat(failed.isFailure).isTrue()
        assertThat(retry.isSuccess).isTrue()
        assertThat(exportedFilesDeletions).isEqualTo(1)
    }

    @Test
    fun runningTwiceOnEmptyDataSucceeds() = runTest {
        useCase()()

        assertThat(useCase()().isSuccess).isTrue()
    }

    @Test
    fun clearsTheReportsOfTheKeptJobDescription() = runTest {
        val kept = KeptJobDescription(text = "jd", company = "Acme", role = "Analyst")
        session.keepJobDescription(kept)
        contentReports.report(
            ContentReport(kept.draftKey, ReportedItemKind.RESUME_BULLET, "b1", "text", Instant.fromEpochSeconds(1)),
        )

        val result = useCase()()

        assertThat(result.isSuccess).isTrue()
        assertThat(contentReports.observeReports(kept.draftKey).first()).isEmpty()
    }

    @Test
    fun removesLegacyPrepPlanStoredUnderTheJobDraftKey() = runTest {
        val kept = KeptJobDescription(text = "jd", company = "Acme", role = "Analyst")
        session.keepJobDescription(kept)
        store.write("prep.plan.${kept.draftKey}", "plan")
        store.write("coverletter.${canonicalApplication.id}", "letter")

        useCase()()

        assertThat(store.read("prep.plan.${kept.draftKey}")).isNull()
        assertThat(store.read("coverletter.${canonicalApplication.id}")).isNull()
    }
}
