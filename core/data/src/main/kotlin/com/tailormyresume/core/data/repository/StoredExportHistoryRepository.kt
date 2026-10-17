package com.tailormyresume.core.data.repository

import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.observeValue
import com.tailormyresume.core.data.mock.readValue
import com.tailormyresume.core.data.mock.writeValue
import com.tailormyresume.core.model.CreditKind
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.ExportRecord
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
internal class StoredExportHistoryRepository @Inject constructor(
    private val store: MockStateStore,
    private val workFence: LocalWorkFence = LocalWorkFence(),
) : ExportHistoryRepository {

    private val mutex = Mutex()
    private val serializer = ListSerializer(ExportDto.serializer())

    override fun observeExports(): Flow<List<ExportRecord>> =
        store.observeValue(EXPORTS_KEY, serializer).map { dtos -> dtos.orEmpty().mapNotNull(ExportDto::toModel) }

    override fun observeExports(applicationId: String): Flow<List<ExportRecord>> =
        observeExports().map { records -> records.filter { it.applicationId == applicationId } }

    override suspend fun record(export: ExportRecord) = workFence.mutate { update { current -> current + export.toDto() } }

    override suspend fun clearFor(applicationId: String) =
        update { current -> current.filterNot { it.applicationId == applicationId } }

    override suspend fun clear() {
        mutex.withLock { store.remove(EXPORTS_KEY) }
    }

    private suspend fun update(transform: (List<ExportDto>) -> List<ExportDto>) {
        mutex.withLock {
            val current = store.readValue(EXPORTS_KEY, serializer).orEmpty()
            store.writeValue(EXPORTS_KEY, serializer, transform(current))
        }
    }

    private companion object {
        const val EXPORTS_KEY = "exports.history"
    }
}

@Serializable
private data class ExportDto(
    val applicationId: String,
    val format: String,
    val fileName: String,
    val exportedAtMillis: Long,
    val creditKind: String?,
    val pageCount: Int? = null,
    val templateName: String? = null,
) {
    fun toModel(): ExportRecord? {
        val exportFormat = ExportFormat.entries.find { it.name == format } ?: return null
        return ExportRecord(
            applicationId = applicationId,
            format = exportFormat,
            fileName = fileName,
            exportedAt = Instant.fromEpochMilliseconds(exportedAtMillis),
            creditKind = CreditKind.entries.find { it.name == creditKind },
            pageCount = pageCount,
            templateName = templateName,
        )
    }
}

private fun ExportRecord.toDto() = ExportDto(
    applicationId = applicationId,
    format = format.name,
    fileName = fileName,
    exportedAtMillis = exportedAt.toEpochMilliseconds(),
    creditKind = creditKind?.name,
    pageCount = pageCount,
    templateName = templateName,
)
