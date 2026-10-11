package com.tailormyresume.core.domain

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeptJobDescription
import javax.inject.Inject
import kotlin.time.Clock

class CreateApplicationUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val tailorResume: TailorResumeUseCase,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        profile: CandidateProfile,
        analysis: JobAnalysisResult,
        kept: KeptJobDescription? = null,
        applicationId: String = idGenerator.newId(),
    ): String {
        val job = kept?.let { analysis.job.withKeptLabel(it) } ?: analysis.job
        val tailored = tailorResume(profile, job, analysis.gap, applicationId)
        val now = clock.now()
        val application = JobApplication(
            id = applicationId,
            job = job,
            status = ApplicationStatus.SAVED,
            gapAnalysis = analysis.gap,
            tailoredResume = tailored,
            createdAt = now,
            updatedAt = now,
        )
        applicationRepository.upsertApplication(application)
        return application.id
    }

    private fun JobDescription.withKeptLabel(kept: KeptJobDescription) = copy(
        title = kept.resolvedTitle(title).trim(),
        company = kept.resolvedCompany(company).trim(),
    )
}
