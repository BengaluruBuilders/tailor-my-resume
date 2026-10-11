package com.tailormyresume.core.domain.account

import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockOperation
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.factCounts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val sessionRepository: SessionRepository,
    private val signInGateway: SignInGateway,
    private val serverAccountDeleter: ServerAccountDeleter,
    private val creditBalance: AccountCreditBalance,
    private val latency: MockLatency,
    private val creditsRepository: CreditsRepository,
    private val resumeSettingsRepository: ResumeSettingsRepository,
    private val exportedFiles: ExportedFiles = ExportedFiles.None,
    private val pendingWipe: PendingAccountWipe = PendingAccountWipe.None,
    private val finishPendingWipe: FinishPendingAccountWipeUseCase =
        FinishPendingAccountWipeUseCase(pendingWipe, AccountWipeFinisher.None, serverAccountDeleter),
) {

    suspend fun hasServerClosedPendingWipe(): Boolean = pendingWipe.state() == PendingWipeState.SERVER_CLOSED

    suspend fun finishRemoval(): PendingWipeOutcome = finishPendingWipe()

    suspend fun preview(): AccountDeletionCounts = previewWith(creditBalance.cachedCredits())

    suspend fun refreshedPreview(): AccountDeletionCounts = previewWith(creditBalance.unusedCredits())

    private suspend fun previewWith(credits: Int): AccountDeletionCounts {
        val applications = applicationRepository.observeApplications().first()
        val profile = profileRepository.observeProfile().first()
        return countsOf(applications, profile, credits)
    }

    suspend operator fun invoke(
        onStep: suspend (AccountDeletionStep) -> Unit = {},
    ): AccountDeletionResult {
        val applications = applicationRepository.observeApplications().first()
        val profile = profileRepository.observeProfile().first()
        val exports = exportHistoryRepository.observeExports().first()
        val counts = countsOf(applications, profile, creditBalance.unusedCredits())
        val tracksPendingWipe = serverAccountDeleter.deletesRemoteData
        if (tracksPendingWipe && serverAlreadyClosedThisAccount()) return settlePendingWipe(counts, dataIntactIfOpen = false)
        val earlierAttempt = tracksPendingWipe && hasEarlierRequestedAttempt()
        if (tracksPendingWipe && !earlierAttempt && !markRequested()) return AccountDeletionResult.Failed(dataIntact = true)
        val deleteFailure = serverAccountDeleter.delete().exceptionOrNull()
        if (deleteFailure != null) {
            val replyMayBeLost = tracksPendingWipe && serverAccountDeleter.mayHaveReachedServer(deleteFailure)
            if (earlierAttempt || replyMayBeLost) return settlePendingWipe(counts, dataIntactIfOpen = true)
            if (tracksPendingWipe) clearMarkerQuietly()
            return AccountDeletionResult.Failed(dataIntact = true)
        }
        var creditsTouched = false
        var artefactsTouched = false
        return try {
            if (tracksPendingWipe) withContext(NonCancellable) { pendingWipe.markServerClosed() }
            startStep(AccountDeletionStep.DELETING_PROFILE_FACTS, onStep)
            profileRepository.clearProfile()
            exportHistoryRepository.clear()
            startStep(AccountDeletionStep.DELETING_APPLICATIONS, onStep)
            applications.forEach { application ->
                applicationRepository.deleteApplicationRow(application.id)
            }
            artefactsTouched = true
            applications.forEach { application -> applicationRepository.clearArtefacts(application.id) }
            startStep(AccountDeletionStep.CLOSING_ACCOUNT, onStep)
            creditsTouched = true
            creditBalance.clearUnusedCredits()
            creditsRepository.clear()
            resumeSettingsRepository.clear()
            withContext(NonCancellable) {
                signInGateway.signOut()
                sessionRepository.clear()
                exportedFiles.deleteAll()
                if (tracksPendingWipe) pendingWipe.clear()
            }
            AccountDeletionResult.Deleted(counts)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            if (tracksPendingWipe) {
                settlePendingWipe(counts, dataIntactIfOpen = false)
            } else {
                val restored = restore(applications, profile, exports, onStep)
                AccountDeletionResult.Failed(dataIntact = restored && !creditsTouched && !artefactsTouched)
            }
        }
    }

    private suspend fun settlePendingWipe(counts: AccountDeletionCounts, dataIntactIfOpen: Boolean): AccountDeletionResult =
        when (finishPendingWipe()) {
            PendingWipeOutcome.FINISHED -> AccountDeletionResult.Deleted(counts)
            PendingWipeOutcome.STILL_PENDING -> AccountDeletionResult.LocalWipePending
            PendingWipeOutcome.NOTHING_PENDING, PendingWipeOutcome.ACCOUNT_KEPT ->
                AccountDeletionResult.Failed(dataIntact = dataIntactIfOpen)
        }

    private suspend fun serverAlreadyClosedThisAccount(): Boolean = try {
        val markerOwner = pendingWipe.uid()
        pendingWipe.state() == PendingWipeState.SERVER_CLOSED &&
            markerOwner != null &&
            markerOwner == signInGateway.currentAccount()?.id
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        false
    }

    private suspend fun hasEarlierRequestedAttempt(): Boolean = try {
        val markerOwner = pendingWipe.uid()
        val currentId = signInGateway.currentAccount()?.id
        val adopts = pendingWipe.state() == PendingWipeState.REQUESTED &&
            (markerOwner == null || markerOwner == currentId)
        if (adopts && markerOwner == null && currentId != null) {
            withContext(NonCancellable) { pendingWipe.recordUid(currentId) }
        }
        adopts
    } catch (failure: Exception) {
        false
    }

    private suspend fun markRequested(): Boolean = try {
        withContext(NonCancellable) {
            val uid = signInGateway.currentAccount()?.id
            if (uid == null) pendingWipe.markRequested() else pendingWipe.markRequested(uid)
        }
        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        false
    }

    private suspend fun clearMarkerQuietly() {
        try {
            withContext(NonCancellable) { pendingWipe.clear() }
        } catch (failure: Exception) {
            return
        }
    }

    private suspend fun startStep(step: AccountDeletionStep, onStep: suspend (AccountDeletionStep) -> Unit) {
        onStep(step)
        latency.await(MockOperation.DELETE_ACCOUNT_STEP)
    }

    private suspend fun countsOf(
        applications: List<JobApplication>,
        profile: CandidateProfile?,
        credits: Int,
    ) = AccountDeletionCounts(
        profileFacts = profile?.factCounts()?.total ?: 0,
        applications = applications.size,
        unusedCredits = credits,
    )

    private suspend fun restore(
        applications: List<JobApplication>,
        profile: CandidateProfile?,
        exports: List<ExportRecord>,
        onStep: suspend (AccountDeletionStep) -> Unit,
    ): Boolean = try {
        onStep(AccountDeletionStep.DELETING_APPLICATIONS)
        applications.forEach { application ->
            applicationRepository.upsertApplication(application)
        }
        if (profile != null) {
            profileRepository.saveProfile(profile)
        }
        exportHistoryRepository.clear()
        exports.forEach { record -> exportHistoryRepository.record(record) }
        true
    } catch (failure: Exception) {
        false
    }
}
