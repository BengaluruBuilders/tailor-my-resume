package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.PendingAccountWipe
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountEarlierAttemptTest {

    private val session = TestSessionRepository().apply {
        sendAccount(SignInAccount(id = "uid-a", displayName = "A", email = "a@example.com"))
    }
    private val profile = TestProfileRepository().apply { sendProfile(canonicalCandidateProfile) }
    private val marker = TestPendingAccountWipe()
    private val finisher = CountingFinisher()
    private var deleteResult: Result<Unit> = Result.success(Unit)
    private var probe: Result<Boolean> = Result.success(true)
    private var uidAtServerCall: String? = null

    private val deleter = object : ServerAccountDeleter {
        override val deletesRemoteData = true

        override suspend fun delete(): Result<Unit> {
            uidAtServerCall = marker.markerUid
            return deleteResult
        }

        override suspend fun isClosed(): Result<Boolean> = probe
    }

    private fun useCase(pendingWipe: PendingAccountWipe = marker) = DeleteAccountUseCase(
        applicationRepository = TestApplicationRepository(),
        profileRepository = profile,
        exportHistoryRepository = TestExportHistoryRepository(),
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = deleter,
        creditBalance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(2)),
        latency = NoMockLatency,
        creditsRepository = TestCreditsRepository(),
        resumeSettingsRepository = TestResumeSettingsRepository(),
        pendingWipe = pendingWipe,
        finishPendingWipe = FinishPendingAccountWipeUseCase(pendingWipe, finisher, deleter),
    )

    @Test
    fun theMarkerNamesTheAccountBeingDeletedBeforeTheServerCall() = runTest {
        useCase()()

        assertThat(uidAtServerCall).isEqualTo("uid-a")
    }

    @Test
    fun anEarlierRequestedMarkerSurvivesAnUnauthenticatedDelete() = runTest {
        marker.current = PendingWipeState.REQUESTED
        marker.markerUid = "uid-a"
        deleteResult = Result.failure(IllegalStateException("401"))
        probe = Result.failure(IllegalStateException("401"))

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.LocalWipePending)
        assertThat(marker.current).isEqualTo(PendingWipeState.REQUESTED)
        assertThat(finisher.runs).isEqualTo(0)
        assertThat(profile.observeProfile().first()).isEqualTo(canonicalCandidateProfile)
    }

    @Test
    fun anEarlierRequestedMarkerIsOnlyClearedWhenTheProbeConfirmsTheAccountIsOpen() = runTest {
        marker.current = PendingWipeState.REQUESTED
        marker.markerUid = "uid-a"
        deleteResult = Result.failure(IllegalStateException("offline"))
        probe = Result.success(false)

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun anEarlierRequestedMarkerForAClosedAccountFinishesTheWipe() = runTest {
        marker.current = PendingWipeState.REQUESTED
        marker.markerUid = "uid-a"
        deleteResult = Result.failure(IllegalStateException("401"))
        probe = Result.success(true)

        val result = useCase()()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(finisher.runs).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun deathBetweenTheSessionClearAndTheMarkerClearLeavesAMarkerThatARestartFinishes() = runTest {
        val dyingOnClear = object : PendingAccountWipe by marker {
            override suspend fun clear() = throw CancellationException("process died")
        }

        val died = runCatching { useCase(dyingOnClear)() }.exceptionOrNull()

        assertThat(died).isInstanceOf(CancellationException::class.java)
        assertThat(session.observeAccount().first()).isNull()
        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)
        assertThat(marker.markerUid).isEqualTo("uid-a")

        val outcome = FinishPendingAccountWipeUseCase(marker, finisher, deleter)()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.FINISHED)
        assertThat(finisher.runs).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    private class CountingFinisher : AccountWipeFinisher {
        var runs = 0

        override suspend fun finish() {
            runs++
        }
    }
}
