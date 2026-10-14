package com.tailormyresume.app.account

import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.data.repository.StoredContentReportRepository
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.mapper.toRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteContentReportRepository @Inject constructor(
    private val local: StoredContentReportRepository,
    private val queue: PendingReportQueue,
    private val api: TailorMyResumeApi,
    @ApplicationScope private val scope: CoroutineScope,
) : ContentReportRepository by local {

    private val flushMutex = Mutex()

    init {
        scope.launch { flush() }
    }

    override suspend fun report(report: ContentReport) {
        local.report(report)
        queue.add(report)
        scope.launch { flush() }
    }

    suspend fun flush() = flushMutex.withLock {
        queue.pending().forEach { report ->
            val failure = apiResult { api.reportContent(report.toRequest()) }.exceptionOrNull()
            if (failure == null || (failure as ApiException).error.isPermanent()) queue.remove(report)
        }
    }

    private fun ApiError.isPermanent() = when (this) {
        ApiError.InvalidInput, ApiError.PayloadTooLarge, ApiError.Forbidden, ApiError.CrossAppToken,
        ApiError.NotFound, ApiError.AppIdInvalid, ApiError.AppNotFound, ApiError.AccountDeleted,
        -> true
        else -> false
    }
}
