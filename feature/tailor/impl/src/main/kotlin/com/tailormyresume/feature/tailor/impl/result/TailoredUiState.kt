package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.runtime.Immutable
import com.tailormyresume.core.designsystem.component.content.TmrPaperBlock

internal enum class TailoredTab { Resume, Changes }

internal sealed interface TailoredUiState {
    data object Loading : TailoredUiState

    data object NotFound : TailoredUiState

    @Immutable
    data class Ready(
        val jobTitle: String,
        val company: String,
        val name: String,
        val contact: String,
        val blocks: List<TmrPaperBlock>,
        val coveragePercent: Int,
        val changes: List<ChangeCard>,
        val accepted: Boolean,
        val exportEnabled: Boolean,
    ) : TailoredUiState
}
