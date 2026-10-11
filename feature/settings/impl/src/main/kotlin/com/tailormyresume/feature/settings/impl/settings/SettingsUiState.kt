package com.tailormyresume.feature.settings.impl.settings

import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import java.io.File

internal sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val email: String,
        val credits: Int,
        val pageSize: PageSize,
        val fileNameFormat: FileNameFormat,
        val productUpdates: Boolean,
    ) : SettingsUiState
}

internal sealed interface SettingsEvent {
    data class ShareArchive(val file: File) : SettingsEvent

    data object DownloadFailed : SettingsEvent
}
