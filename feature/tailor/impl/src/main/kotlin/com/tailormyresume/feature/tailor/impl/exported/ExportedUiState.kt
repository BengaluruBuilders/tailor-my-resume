package com.tailormyresume.feature.tailor.impl.exported

import androidx.compose.runtime.Immutable
import com.tailormyresume.core.model.ApplicationStatus
import java.io.File

internal sealed interface ExportedUiState {
    data object Loading : ExportedUiState

    data object NothingExported : ExportedUiState

    data object ExportFailed : ExportedUiState

    @Immutable
    data class Ready(
        val fileName: String,
        val pageCount: Int,
        val sizeKb: Int,
        val creditsLeft: Int,
        val freeResumeUsed: Boolean,
        val jobTitle: String,
        val company: String,
        val status: ApplicationStatus,
        val markedOn: String?,
        val statusSheet: ApplicationStatus?,
    ) : ExportedUiState
}

internal sealed interface ExportedEvent {
    data class Share(val file: File) : ExportedEvent

    data class Open(val file: File) : ExportedEvent

    data object FileMissing : ExportedEvent

    data object ComingSoon : ExportedEvent

    data class MarkedApplied(val on: String) : ExportedEvent

    data class StatusSet(val status: ApplicationStatus) : ExportedEvent
}
