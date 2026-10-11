package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountExportedFilesTest {

    private var deleteAllCalls = 0
    private val exportedFiles = ExportedFiles { deleteAllCalls++ }
    private val session = TestSessionRepository()
    private val profiles = TestProfileRepository()

    private fun useCase(serverAccountDeleter: ServerAccountDeleter = OfflineServerAccountDeleter()) = DeleteAccountUseCase(
        applicationRepository = TestApplicationRepository(),
        profileRepository = profiles,
        exportHistoryRepository = TestExportHistoryRepository(),
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = serverAccountDeleter,
        creditBalance = AccountCreditBalance(paymentGateway = TestPaymentGateway()),
        latency = NoMockLatency,
        creditsRepository = TestCreditsRepository(),
        resumeSettingsRepository = TestResumeSettingsRepository(),
        exportedFiles = exportedFiles,
    )

    @Test
    fun deletesExportedFilesOnSuccess() = runTest {
        profiles.saveProfile(canonicalCandidateProfile)

        val result = useCase()()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(deleteAllCalls).isEqualTo(1)
    }

    @Test
    fun keepsExportedFilesWhenTheServerRefusesTheDeletion() = runTest {
        val result = useCase(ServerAccountDeleter { Result.failure(IllegalStateException("offline")) })()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(deleteAllCalls).isEqualTo(0)
    }
}
