package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountArtefactRestoreTest {

    private val applications = listOf("a1", "a2", "a3").map { id ->
        sampleApplication.copy(id = id, status = ApplicationStatus.SAVED)
    }
    private val rows = TestApplicationRepository().apply { sendApplications(applications) }
    private val repository = ArtefactTrackingRepository(rows)
    private val profile = TestProfileRepository().apply { sendProfile(canonicalCandidateProfile) }

    private fun useCase() = DeleteAccountUseCase(
        applicationRepository = repository,
        profileRepository = profile,
        exportHistoryRepository = TestExportHistoryRepository(),
        sessionRepository = TestSessionRepository(),
        signInGateway = TestSignInGateway(TestSessionRepository()),
        serverAccountDeleter = OfflineServerAccountDeleter(),
        creditBalance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(2)),
        latency = NoMockLatency,
        creditsRepository = TestCreditsRepository(),
        resumeSettingsRepository = TestResumeSettingsRepository(),
    )

    @Test
    fun rowDeleteFailureKeepsEveryApplicationsArtefacts() = runTest {
        repository.failRowDeleteFor = "a2"

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(repository.artefacts.keys).containsExactly("a1", "a2", "a3")
        assertThat(repository.artefacts.values.flatten().toSet()).containsExactly("letter", "prep", "review", "reports")
    }

    @Test
    fun restoreAfterFailureLeavesLettersReviewStateReportsAndPrepPlan() = runTest {
        repository.failRowDeleteFor = "a3"

        useCase()()

        assertThat(rows.observeApplications().first().map(JobApplication::id)).containsExactly("a1", "a2", "a3")
        applications.forEach { application ->
            assertThat(repository.artefacts[application.id]).containsExactly("letter", "prep", "review", "reports")
        }
    }

    @Test
    fun restoreKeepsLegacyNotesAndStatusAndStaysIntact() = runTest {
        rows.sendApplications(applications.map { it.copy(legacyNotes = "note ${it.id}", legacyStatus = "NO_RESPONSE") })
        repository.failRowDeleteFor = "a3"

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        val restored = rows.observeApplications().first().associateBy(JobApplication::id)
        applications.forEach { application ->
            assertThat(restored.getValue(application.id).legacyNotes).isEqualTo("note ${application.id}")
            assertThat(restored.getValue(application.id).legacyStatus).isEqualTo("NO_RESPONSE")
        }
    }

    @Test
    fun artefactsClearedOnlyAfterEveryRowDeleteSucceeds() = runTest {
        val result = useCase()()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(repository.log).containsExactly("row:a1", "row:a2", "row:a3", "clear:a1", "clear:a2", "clear:a3").inOrder()
    }

    @Test
    fun artefactsClearedBeforeTheClosingAccountStep() = runTest {
        useCase()(onStep = { repository.log += "step:$it" })

        val log = repository.log
        assertThat(log.indexOf("clear:a3")).isLessThan(log.indexOf("step:CLOSING_ACCOUNT"))
    }

    @Test
    fun artefactCleanupFailureReportsDataNotIntact() = runTest {
        repository.failClearFor = "a2"

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = false))
        assertThat(rows.observeApplications().first().map(JobApplication::id)).containsExactly("a1", "a2", "a3")
    }

    @Test
    fun successLeavesNoArtefactKeys() = runTest {
        useCase()()

        assertThat(repository.artefacts).isEmpty()
        assertThat(rows.observeApplications().first()).isEmpty()
    }

    private class ArtefactTrackingRepository(
        private val rows: TestApplicationRepository,
    ) : ApplicationRepository by rows {
        val artefacts = mutableMapOf<String, Set<String>>().apply {
            listOf("a1", "a2", "a3").forEach { put(it, setOf("letter", "prep", "review", "reports")) }
        }
        val log = mutableListOf<String>()
        var failRowDeleteFor: String? = null
        var failClearFor: String? = null

        override suspend fun deleteApplication(id: String) {
            clearArtefacts(id)
            deleteApplicationRow(id)
        }

        override suspend fun deleteApplicationRow(id: String) {
            if (id == failRowDeleteFor) throw IllegalStateException("row delete failed")
            log += "row:$id"
            rows.deleteApplication(id)
        }

        override suspend fun clearArtefacts(id: String) {
            if (id == failClearFor) throw IllegalStateException("clear failed")
            log += "clear:$id"
            artefacts.remove(id)
        }
    }
}
