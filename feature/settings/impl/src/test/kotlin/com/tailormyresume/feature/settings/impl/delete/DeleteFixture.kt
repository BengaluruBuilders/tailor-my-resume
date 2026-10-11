package com.tailormyresume.feature.settings.impl.delete

import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.DeleteAccountUseCase
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.domain.account.FinishPendingAccountWipeUseCase
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first

internal class RecordingServerAccountDeleter(
    var failure: Throwable? = null,
    override val deletesRemoteData: Boolean = false,
    private val gate: CompletableDeferred<Unit>? = null,
) : ServerAccountDeleter {
    var deleteCalls = 0

    override suspend fun delete(): Result<Unit> {
        deleteCalls += 1
        gate?.await()
        return failure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun isClosed(): Result<Boolean> = Result.failure(IllegalStateException("unknown"))

    override fun mayHaveReachedServer(failure: Throwable): Boolean = deletesRemoteData
}

internal class DeleteFixture(
    private val applications: Int = 3,
    private val credits: Int = 2,
    val deleter: RecordingServerAccountDeleter = RecordingServerAccountDeleter(),
    private val payment: PaymentGateway = TestPaymentGateway().withFreeCredits(credits),
    private val finisher: AccountWipeFinisher = AccountWipeFinisher.None,
) {
    val applicationRepository = TestApplicationRepository()
    val pendingWipe = TestPendingAccountWipe()
    var exportedFilesCleared = 0
    private val session = TestSessionRepository()

    suspend fun seed(): DeleteFixture = apply {
        repeat(applications) { index ->
            applicationRepository.upsertApplication(sampleApplication.copy(id = "application-$index"))
        }
    }

    fun signedInWithServerClosedMarker(uid: String): DeleteFixture = apply {
        session.sendAccount(SignInAccount(id = uid, displayName = "Account", email = "account@example.com"))
        pendingWipe.current = PendingWipeState.SERVER_CLOSED
        pendingWipe.markerUid = uid
    }

    suspend fun remainingApplications(): Int = applicationRepository.observeApplications().first().size

    fun useCase(): DeleteAccountUseCase = DeleteAccountUseCase(
        applicationRepository = applicationRepository,
        profileRepository = TestProfileRepository(),
        exportHistoryRepository = TestExportHistoryRepository(),
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = deleter,
        creditBalance = AccountCreditBalance(payment),
        latency = NoMockLatency,
        creditsRepository = TestCreditsRepository(),
        resumeSettingsRepository = TestResumeSettingsRepository(),
        exportedFiles = ExportedFiles { exportedFilesCleared += 1 },
        pendingWipe = pendingWipe,
        finishPendingWipe = FinishPendingAccountWipeUseCase(pendingWipe, finisher, deleter),
    )

    fun viewModel(): DeleteAccountViewModel = DeleteAccountViewModel(useCase())
}
