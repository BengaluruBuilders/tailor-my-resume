package com.tailormyresume.app.auth

import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.domain.account.TransientDataCleaner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteTransientDataCleaner @Inject constructor(
    private val analysis: RemoteJobAnalysisSource,
    private val reports: PendingReportQueue,
) : TransientDataCleaner {
    override suspend fun clear() {
        analysis.clear()
        reports.clear()
    }
}
