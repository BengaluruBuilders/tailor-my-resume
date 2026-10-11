package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.TailoredResume
import javax.inject.Inject

class GuardedResumeTailor @Inject constructor(
    private val remote: RemoteResumeTailor,
    private val offline: OfflineResumeTailor,
    private val previewMode: DebugPreviewMode,
) : ResumeTailor {
    override suspend fun tailor(
        profile: CandidateProfile,
        job: JobDescription,
        gap: GapAnalysis,
        applicationId: String,
        answer: QuickAnswer?,
    ): TailoredResume =
        if (previewMode.active) {
            offline.tailor(profile, job, gap, applicationId, answer)
        } else {
            remote.tailor(profile, job, gap, applicationId, answer)
        }
}
