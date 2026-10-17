package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.TailoredResume

fun JobApplication.withFinalMeasuredOn(resume: TailoredResume, profile: CandidateProfile): JobApplication {
    val gap = gapAnalysis ?: return this
    return copy(keywordCoverage = KeywordCoverageCalculator.compute(gap.matches, quickAnswer, resume, profile))
}
