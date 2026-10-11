package com.tailormyresume.feature.applications.impl

import androidx.compose.runtime.Immutable
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus

@Immutable
internal data class ApplicationsHeader(val credits: Int, val initials: String)

@Immutable
internal data class ApplicationRow(
    val id: String,
    val title: String,
    val company: String,
    val keywordPercent: Int?,
    val initial: String,
    val colorIndex: Int,
    val status: TmrApplicationStatus,
)

internal sealed interface ApplicationsUiState {
    data object Loading : ApplicationsUiState

    data class Empty(val header: ApplicationsHeader) : ApplicationsUiState

    @Immutable
    data class Populated(
        val header: ApplicationsHeader,
        val total: Int,
        val statusCounts: Map<TmrApplicationStatus, Int>,
        val rows: List<ApplicationRow>,
    ) : ApplicationsUiState
}

internal const val DISC_COLOR_COUNT = 3
