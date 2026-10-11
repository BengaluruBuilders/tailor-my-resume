package com.tailormyresume.feature.tailor.impl.edit

import androidx.compose.runtime.Immutable
import com.tailormyresume.feature.tailor.impl.result.ChangeSource

internal sealed interface EditResumeUiState {
    data object Loading : EditResumeUiState

    data object NotFound : EditResumeUiState

    @Immutable
    data class Ready(
        val summary: String,
        val roles: List<EditRole>,
        val skills: String,
        val fitsOnOnePage: Boolean,
    ) : EditResumeUiState
}

@Immutable
internal data class EditRole(val label: String, val bullets: List<EditBullet>)

@Immutable
internal data class EditBullet(
    val id: String?,
    val text: String,
    val source: ChangeSource,
)

internal sealed interface EditResumeEvent {
    data object Saved : EditResumeEvent
}
