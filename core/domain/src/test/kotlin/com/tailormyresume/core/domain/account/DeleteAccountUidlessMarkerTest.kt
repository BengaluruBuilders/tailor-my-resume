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
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountUidlessMarkerTest {

    private val session = TestSessionRepository().apply {
        sendAccount(SignInAccount(id = "uid-a", displayName = "A", email = "a@example.com"))
    }
    private val marker = TestPendingAccountWipe()
    private val requestedWith = mutableListOf<String>()
    private var recordedUids = 0
    private var uidAtServerCall: String? = null
    private var deleteResult: Result<Unit> = Result.success(Unit)

    private val observedMarker = object : PendingAccountWipe by marker {
        override suspend fun markRequested(uid: String) {
            requestedWith += uid
            marker.markRequested(uid)
        }

        override suspend fun recordUid(uid: String) {
            recordedUids++
            marker.recordUid(uid)
        }
    }

    private val deleter = object : ServerAccountDeleter {
        override val deletesRemoteData = true

        override suspend fun delete(): Result<Unit> {
            uidAtServerCall = marker.markerUid
            return deleteResult
        }

        override suspend fun isClosed(): Result<Boolean> = Result.failure(IllegalStateException("probe down"))
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
        pendingWipe = observedMarker,
        finishPendingWipe = FinishPendingAccountWipeUseCase(
            observedMarker,
            object : AccountWipeFinisher {
                override suspend fun finish() = Unit
            },
            deleter,
        ),
    )

    @Test
    fun markerNamesTheAccountInOneCall() = runTest {
        useCase()()

        assertThat(requestedWith).containsExactly("uid-a")
        assertThat(recordedUids).isEqualTo(0)
    }

    @Test
    fun adoptingAUidlessMarkerRecordsTheCurrentUid() = runTest {
        marker.current = PendingWipeState.REQUESTED
        marker.markerUid = null
        deleteResult = Result.failure(IllegalStateException("401"))

        val result = useCase()()

        assertThat(result).isEqualTo(AccountDeletionResult.LocalWipePending)
        assertThat(uidAtServerCall).isEqualTo("uid-a")
        assertThat(marker.markerUid).isEqualTo("uid-a")
    }
}
