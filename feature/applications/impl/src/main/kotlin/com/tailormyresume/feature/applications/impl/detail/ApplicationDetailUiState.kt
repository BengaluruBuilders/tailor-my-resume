package com.tailormyresume.feature.applications.impl.detail

import android.content.Intent
import androidx.compose.runtime.Immutable
import com.tailormyresume.core.model.ApplicationStatus

@Immutable
internal data class ApplicationFile(val fileName: String, val keywordPercent: Int?)

internal sealed interface ApplicationDetailUiState {
    data object Loading : ApplicationDetailUiState

    data object NotFound : ApplicationDetailUiState

    @Immutable
    data class Ready(
        val title: String,
        val company: String,
        val place: String,
        val status: ApplicationStatus,
        val appliedOn: String?,
        val file: ApplicationFile?,
        val statusSheet: ApplicationStatus?,
        val canReviewTailoredResume: Boolean = false,
    ) : ApplicationDetailUiState
}

internal sealed interface ApplicationDetailEvent {
    data class ShareFile(val intent: Intent) : ApplicationDetailEvent

    data class OpenFile(val intent: Intent) : ApplicationDetailEvent

    data object FileMissing : ApplicationDetailEvent

    data object BalanceUnavailable : ApplicationDetailEvent

    data class StatusSet(val status: ApplicationStatus) : ApplicationDetailEvent

    data class OpenQuickQuestion(val applicationId: String) : ApplicationDetailEvent

    data class StartTailoring(val applicationId: String) : ApplicationDetailEvent

    data class ReviewTailoredResume(val applicationId: String) : ApplicationDetailEvent

    data class EarnCredits(val applicationId: String) : ApplicationDetailEvent
}
