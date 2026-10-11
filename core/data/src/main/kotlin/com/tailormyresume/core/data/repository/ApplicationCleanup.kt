package com.tailormyresume.core.data.repository

import com.tailormyresume.core.data.mock.MockStateStore
import javax.inject.Inject

internal fun interface ApplicationCleanup {
    suspend fun clearFor(applicationId: String)
}

internal class StoredApplicationCleanup @Inject constructor(
    private val reports: ContentReportRepository,
    private val reviewState: TailoringReviewStateRepository,
    private val store: MockStateStore,
) : ApplicationCleanup {
    override suspend fun clearFor(applicationId: String) {
        reports.clearFor(applicationId)
        reviewState.clearFor(applicationId)
        store.remove("coverletter.$applicationId")
        store.remove("prep.plan.$applicationId")
    }
}
