package com.tailormyresume.feature.tailor.impl.export

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.domain.ExportCheck
import com.tailormyresume.core.domain.ExportReadiness
import com.tailormyresume.core.domain.export.ExportFileName
import com.tailormyresume.core.domain.export.ExportFileNameFormat
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock

internal sealed interface ExportOutcome {
    data class Exported(
        val fileName: String,
        val file: File,
        val pageCount: Int,
        val sizeBytes: Long,
    ) : ExportOutcome

    data object Refused : ExportOutcome

    data object Failed : ExportOutcome
}

internal class ExportResumeUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: ResumeSettingsRepository,
    private val exportHistory: ExportHistoryRepository,
    private val assembler: ResumeDocumentAssembler,
    private val renderer: ResumePdfRenderer,
    private val clock: Clock,
) {
    suspend operator fun invoke(applicationId: String): ExportOutcome {
        val application = applicationRepository.observeApplication(applicationId).first() ?: return ExportOutcome.Refused
        if (ExportReadiness.check(application) != ExportCheck.ALLOWED) return ExportOutcome.Refused
        val resume = application.tailoredResume ?: return ExportOutcome.Refused
        val exportableResume = if (resume.changeCount == 0) resume.withPendingAccepted() else resume
        val profile = profileRepository.observeProfile().first() ?: return ExportOutcome.Refused
        val settings = settingsRepository.observeSettings().first()
        val fileName = ExportFileName.build(profile, application.job, settings.fileNameFormat.toExportFormat())
        val rendered = renderer.render(
            applicationId,
            assembler.assemble(profile, exportableResume),
            fileName,
            settings.pageSize,
            previousFileName = application.exportFileName,
        )
        val exportedAt = clock.now()
        applicationRepository.observeApplication(applicationId).first()?.let { latest ->
            applicationRepository.upsertApplication(latest.copy(exportFileName = fileName, updatedAt = exportedAt))
        }
        exportHistory.record(
            ExportRecord(
                applicationId = applicationId,
                format = ExportFormat.PDF,
                fileName = fileName,
                exportedAt = exportedAt,
                creditKind = null,
                pageCount = rendered.pageCount,
            ),
        )
        return ExportOutcome.Exported(fileName, rendered.file, rendered.pageCount, rendered.sizeBytes)
    }

    suspend fun storedExport(applicationId: String, fileFor: (String) -> File?): ExportOutcome.Exported? {
        val fileName = applicationRepository.observeApplication(applicationId).first()?.exportFileName ?: return null
        val record = exportHistory.observeExports(applicationId).first()
            .filter { it.format == ExportFormat.PDF && it.fileName == fileName }
            .maxByOrNull { it.exportedAt } ?: return null
        val pageCount = record.pageCount ?: return null
        val file = fileFor(fileName) ?: return null
        return ExportOutcome.Exported(fileName, file, pageCount, file.length())
    }

    private fun FileNameFormat.toExportFormat(): ExportFileNameFormat = when (this) {
        FileNameFormat.NAME_COMPANY_ROLE -> ExportFileNameFormat.NAME_COMPANY_ROLE
        FileNameFormat.NAME_ROLE -> ExportFileNameFormat.NAME_ROLE
        FileNameFormat.NAME_RESUME -> ExportFileNameFormat.NAME_RESUME
    }
}
