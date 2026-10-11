package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.SignInGateway
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

class DeleteAccountPendingWipeTest {

    private val session = TestSessionRepository()
    private val profile = TestProfileRepository().apply { sendProfile(canonicalCandidateProfile) }
    private val marker = TestPendingAccountWipe()
    private val finisher = FlakyFinisher()
    private var signOutFailures = 0
    private var serverResult: Result<Unit> = Result.success(Unit)
    private var markerAtServerCall: PendingWipeState? = null
    private var deletesRemoteData = true
    private var serverCalls = 0

    private val deleter = object : ServerAccountDeleter {
        override val deletesRemoteData get() = this@DeleteAccountPendingWipeTest.deletesRemoteData

        override suspend fun delete(): Result<Unit> {
            serverCalls++
            markerAtServerCall = marker.current
            return serverResult
        }

        override suspend fun isClosed(): Result<Boolean> = Result.success(true)
    }

    private fun useCase(
        pendingWipe: PendingAccountWipe = marker,
        wipeFinisher: AccountWipeFinisher = finisher,
    ): DeleteAccountUseCase {
        val signIn = TestSignInGateway(session)
        val failingSignIn = object : SignInGateway by signIn {
            override suspend fun signOut() {
                if (signOutFailures > 0) {
                    signOutFailures--
                    throw IllegalStateException("sign out failed")
                }
                signIn.signOut()
            }
        }
        return DeleteAccountUseCase(
            applicationRepository = TestApplicationRepository(),
            profileRepository = profile,
            exportHistoryRepository = TestExportHistoryRepository(),
            sessionRepository = session,
            signInGateway = failingSignIn,
            serverAccountDeleter = deleter,
            creditBalance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(2)),
            latency = NoMockLatency,
            creditsRepository = TestCreditsRepository(),
            resumeSettingsRepository = TestResumeSettingsRepository(),
            pendingWipe = pendingWipe,
            finishPendingWipe = FinishPendingAccountWipeUseCase(pendingWipe, wipeFinisher, deleter),
        )
    }

    @Test
    fun markerSetBeforeServerCall() = runTest {
        useCase()()

        assertThat(markerAtServerCall).isEqualTo(PendingWipeState.REQUESTED)
    }

    @Test
    fun successfulDeletionClearsTheMarker() = runTest {
        val result = useCase()()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
        assertThat(finisher.runs).isEqualTo(0)
        assertThat(marker.history).containsExactly(
            PendingWipeState.REQUESTED,
            PendingWipeState.SERVER_CLOSED,
            PendingWipeState.NONE,
        ).inOrder()
    }

    @Test
    fun serverFailureClearsMarker() = runTest {
        serverResult = Result.failure(IllegalStateException("offline"))

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
        assertThat(profile.observeProfile().first()).isEqualTo(canonicalCandidateProfile)
    }

    @Test
    fun aMarkerThatCannotBeWrittenBlocksTheServerCall() = runTest {
        val unwritable = object : PendingAccountWipe by marker {
            override suspend fun markRequested() = throw IllegalStateException("disk full")
        }

        val result = useCase(pendingWipe = unwritable)()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(serverCalls).isEqualTo(0)
    }

    @Test
    fun localFailureAfterServerCloseFinishesWipeInline() = runTest {
        signOutFailures = 1

        val result = useCase()()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(finisher.runs).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun persistentLocalFailureReturnsLocalWipePending() = runTest {
        signOutFailures = 1
        finisher.failuresLeft = Int.MAX_VALUE

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.LocalWipePending)
        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)
    }

    @Test
    fun cancellationAfterServerCloseKeepsMarker() = runTest {
        val thrown = runCatching {
            useCase()(onStep = { step ->
                if (step == AccountDeletionStep.CLOSING_ACCOUNT) throw CancellationException("stop")
            })
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)
    }

    @Test
    fun aRestartAfterCancellationFinishesTheWipe() = runTest {
        runCatching {
            useCase()(onStep = { step ->
                if (step == AccountDeletionStep.CLOSING_ACCOUNT) throw CancellationException("stop")
            })
        }

        val outcome = FinishPendingAccountWipeUseCase(marker, finisher, deleter)()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.FINISHED)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun demoNeverWritesMarker() = runTest {
        deletesRemoteData = false
        signOutFailures = 1

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = false))
        assertThat(marker.history).isEmpty()
        assertThat(finisher.runs).isEqualTo(0)
    }

    private class FlakyFinisher : AccountWipeFinisher {
        var runs = 0
        var failuresLeft = 0

        override suspend fun finish() {
            runs++
            if (failuresLeft > 0) {
                failuresLeft--
                throw IllegalStateException("wipe failed")
            }
        }
    }
}
