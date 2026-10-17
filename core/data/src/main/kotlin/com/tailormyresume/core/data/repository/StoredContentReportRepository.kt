package com.tailormyresume.core.data.repository

import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.observeValue
import com.tailormyresume.core.data.mock.readValue
import com.tailormyresume.core.data.mock.writeValue
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.ReportedItemKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

@Singleton
class StoredContentReportRepository @Inject constructor(
    private val store: MockStateStore,
    private val workFence: LocalWorkFence = LocalWorkFence(),
) : ContentReportRepository {

    private val mutex = Mutex()
    private val serializer = ListSerializer(ReportDto.serializer())

    override suspend fun report(report: ContentReport) = workFence.mutate {
        mutex.withLock {
            val key = keyOf(report.applicationId)
            val current = store.readValue(key, serializer).orEmpty()
            val known = current.any { it.itemKind == report.itemKind.name && it.itemId == report.itemId }
            if (!known) store.writeValue(key, serializer, current + report.toDto())
        }
    }

    override fun observeReports(applicationId: String): Flow<List<ContentReport>> =
        store.observeValue(keyOf(applicationId), serializer).map { dtos -> dtos.orEmpty().mapNotNull(ReportDto::toModel) }

    override fun observeReportedIds(applicationId: String, kind: ReportedItemKind): Flow<Set<String>> =
        observeReports(applicationId).map { reports -> reports.filter { it.itemKind == kind }.map { it.itemId }.toSet() }

    override suspend fun clearFor(applicationId: String) {
        mutex.withLock { store.remove(keyOf(applicationId)) }
    }

    private fun keyOf(applicationId: String) = "reports.content.$applicationId"
}

@Serializable
internal data class ReportDto(
    val applicationId: String,
    val itemKind: String,
    val itemId: String,
    val reportedAtMillis: Long,
    val itemText: String = "",
    val generationId: String? = null,
    val owner: String? = null,
) {
    fun toModel(): ContentReport? {
        val kind = ReportedItemKind.entries.find { it.name == itemKind } ?: return null
        return ContentReport(applicationId, kind, itemId, itemText, Instant.fromEpochMilliseconds(reportedAtMillis), generationId)
    }
}

internal fun ContentReport.toDto() = ReportDto(
    applicationId = applicationId,
    itemKind = itemKind.name,
    itemId = itemId,
    reportedAtMillis = reportedAt.toEpochMilliseconds(),
    itemText = itemText,
    generationId = generationId,
)
