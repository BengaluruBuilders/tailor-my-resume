package com.tailormyresume.core.domain

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.TailoredResume
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Clock

class UpdateBulletDecisionUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(applicationId: String, changeId: String, decision: BulletDecision) =
        DecisionWriteLock.serialised {
            val application = applicationRepository.observeApplication(applicationId).first() ?: return@serialised
            val resume = application.tailoredResume ?: return@serialised
            applicationRepository.upsertApplication(application.withDecision(resume, changeId, decision))
        }

    private fun JobApplication.withDecision(
        resume: TailoredResume,
        changeId: String,
        decision: BulletDecision,
    ): JobApplication {
        val decided = resume.copy(
            bullets = resume.bullets.map { if (it.id == changeId) it.copy(decision = decision) else it },
            summary = resume.summary?.let { if (changeId == SUMMARY_CHANGE_ID) it.copy(decision = decision) else it },
            skills = resume.skills?.let { if (changeId == SKILLS_CHANGE_ID) it.copy(decision = decision) else it },
        )
        return copy(tailoredResume = decided, updatedAt = clock.now())
    }

    companion object {
        const val SUMMARY_CHANGE_ID = "summary"
        const val SKILLS_CHANGE_ID = "skills"
    }
}
