package com.tailormyresume.core.domain.sample

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import com.tailormyresume.core.domain.onboarding.NextOnboardingStepUseCase
import com.tailormyresume.core.domain.onboarding.OnboardingStep
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class OfflineSampleDataController(
    private val store: MockStateStore,
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val applicationRepository: ApplicationRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val paymentGateway: OfflinePaymentGateway,
    private val analyzeJob: AnalyzeJobUseCase,
    private val tailorResume: TailorResumeUseCase,
    private val clock: Clock,
    private val firebaseUid: FirebaseUidProvider = FirebaseUidProvider { null },
) : SampleDataController {

    override suspend fun load() {
        reset()
        val now = clock.now()
        signInSampleCandidate()
        paymentGateway.purchase(SampleDataSet.PURCHASED_PACK_ID)
        SampleDataSet.applications.forEach { plan -> applicationRepository.upsertApplication(build(plan, now)) }
        SampleDataSet.exports.forEach { plan -> recordExport(plan, now) }
        sessionRepository.markOnboardingComplete()
    }

    override suspend fun reset() {
        sessionRepository.clear()
        exportHistoryRepository.clear()
        store.clear()
        profileRepository.clearProfile()
        applicationRepository.observeApplications().first().forEach { application ->
            applicationRepository.deleteApplication(application.id)
        }
    }

    override suspend fun keepSampleJobDescription() {
        sessionRepository.keepJobDescription(SampleDataSet.keptJob)
        val next = NextOnboardingStepUseCase(sessionRepository, profileRepository)()
        if (next !is OnboardingStep.GapAnalysis && firebaseUid.uid() == null) signInSampleCandidate()
    }

    private suspend fun signInSampleCandidate() {
        sessionRepository.saveAccount(SampleDataSet.account)
        profileRepository.saveProfile(SampleDataSet.profile)
    }

    override suspend fun clearSampleJobDescription() {
        sessionRepository.clearKeptJobDescription()
    }

    private suspend fun build(plan: SampleApplicationPlan, now: Instant): JobApplication {
        val analysis = analyzeJob(SampleDataSet.profile, plan.jobText)
        val tailored = tailorResume(SampleDataSet.profile, analysis.job, analysis.gap, plan.id)
        return JobApplication(
            id = plan.id,
            job = analysis.job,
            status = plan.status,
            notes = plan.notes,
            gapAnalysis = analysis.gap,
            tailoredResume = reviewed(tailored, plan.review),
            createdAt = now - plan.createdDaysAgo.days,
            updatedAt = now - plan.updatedDaysAgo.days,
        )
    }

    private fun reviewed(resume: TailoredResume, plan: ReviewPlan): TailoredResume {
        val reviewedCount = when (plan) {
            ReviewPlan.ALL -> resume.bullets.size
            ReviewPlan.PARTIAL -> resume.bullets.size / 2
            ReviewPlan.NONE -> 0
        }
        var changedSeen = 0
        val bullets = resume.bullets.mapIndexed { index, bullet ->
            if (index >= reviewedCount) return@mapIndexed bullet
            val changed = bullet.proposedText != bullet.originalText
            if (changed) changedSeen++
            bullet.decidedAs(if (changed && changedSeen % REJECT_EVERY == 0) BulletDecision.REJECTED else BulletDecision.ACCEPTED)
        }
        return resume.copy(bullets = bullets)
    }

    private fun TailoredBullet.decidedAs(decision: BulletDecision) = copy(decision = decision)

    private suspend fun recordExport(plan: SampleExportPlan, now: Instant) {
        val spend = paymentGateway.unlock(plan.applicationId)
        exportHistoryRepository.record(
            ExportRecord(
                applicationId = plan.applicationId,
                format = plan.format,
                fileName = plan.fileName,
                exportedAt = now - plan.exportedDaysAgo.days,
                creditKind = (spend as? CreditSpend.Spent)?.kind,
            ),
        )
    }

    private companion object {
        const val REJECT_EVERY = 4
    }
}
