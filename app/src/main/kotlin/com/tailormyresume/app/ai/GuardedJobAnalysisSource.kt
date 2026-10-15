package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.model.CandidateProfile
import javax.inject.Inject

class GuardedJobAnalysisSource @Inject constructor(
    private val remote: RemoteJobAnalysisSource,
    private val offline: OfflineJobAnalysisSource,
    private val previewMode: DebugPreviewMode,
) : JobAnalysisSource {
    override suspend fun analyse(profile: CandidateProfile, rawJobText: String): JobAnalysisResult =
        if (previewMode.active) offline.analyse(profile, rawJobText) else remote.analyse(profile, rawJobText)
}
