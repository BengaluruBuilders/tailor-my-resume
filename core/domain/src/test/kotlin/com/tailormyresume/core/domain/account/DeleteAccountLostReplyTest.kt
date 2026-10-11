package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountLostReplyTest {

    private class LostReply : Exception("reply lost")

    private val session = TestSessionRepository().apply {
        sendAccount(SignInAccount(id = "uid-a", displayName = "A", email = "a@example.com"))
    }
    private val marker = TestPendingAccountWipe()
    private var wipes = 0
    private var deleteFailure: Exception = LostReply()
    private var probe: Result<Boolean> = Result.success(true)

    private val deleter = object : ServerAccountDeleter {
        override val deletesRemoteData = true

        override suspend fun delete(): Result<Unit> = Result.failure(deleteFailure)

        override suspend fun isClosed(): Result<Boolean> = probe

        override fun mayHaveReachedServer(failure: Throwable): Boolean = failure is LostReply
    }

    private fun useCase() = DeleteAccountUseCase(
        applicationRepository = TestApplicationRepository(),
        profileRepository = TestProfileRepository().apply { sendProfile(canonicalCandidateProfile) },
        exportHistoryRepository = TestExportHistoryRepository(),
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = deleter,
        creditBalance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(2)),
        latency = NoMockLatency,
        creditsRepository = TestCreditsRepository(),
        resumeSettingsRepository = TestResumeSettingsRepository(),
        pendingWipe = marker,
        finishPendingWipe = FinishPendingAccountWipeUseCase(
            marker,
            object : AccountWipeFinisher {
                override suspend fun finish() {
                    wipes++
                }
            },
            deleter,
        ),
    )

    @Test
    fun timeoutThenProbeClosedReturnsDeleted() = runTest {
        probe = Result.success(true)

        val result = useCase()()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun timeoutThenProbeFailsReturnsLocalWipePendingAndKeepsMarker() = runTest {
        probe = Result.failure(IllegalStateException("offline"))

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.LocalWipePending)
        assertThat(marker.current).isEqualTo(PendingWipeState.REQUESTED)
        assertThat(wipes).isEqualTo(0)
    }

    @Test
    fun timeoutThenProbeOpenClearsMarkerAndFailsIntact() = runTest {
        probe = Result.success(false)

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun unambiguousFailureStillClearsMarker() = runTest {
        deleteFailure = IllegalStateException("400")
        probe = Result.success(true)

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
        assertThat(wipes).isEqualTo(0)
    }
}
