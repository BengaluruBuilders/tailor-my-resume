package com.tailormyresume.core.domain

import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.TailoredResume

interface ResumeTailor {
    suspend fun tailor(
        profile: CandidateProfile,
        job: JobDescription,
        gap: GapAnalysis,
        applicationId: String,
        answer: QuickAnswer?,
    ): TailoredResume
}
