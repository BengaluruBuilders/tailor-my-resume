package com.tailormyresume.feature.tailor.impl.tailoring

import com.tailormyresume.core.designsystem.component.content.TmrProgressState
import com.tailormyresume.core.model.QuickAnswer

private const val CHOICE_YES_REGULARLY = "YES_REGULARLY"
private const val CHOICE_A_FEW_TIMES = "A_FEW_TIMES"
private const val PERCENT_PER_STAGE = 25

internal enum class TailoringRowKind { Matching, Rewriting, AddingExample, CheckingMustHaves, Fitting }

internal data class TailoringRowUi(val kind: TailoringRowKind, val state: TmrProgressState)

internal data class TailoringUiState(
    val percent: Int,
    val rows: List<TailoringRowUi>,
    val keywordCount: Int,
    val stickers: List<String>,
)

internal fun tailoringRows(percent: Int, addsExample: Boolean): List<TailoringRowUi> {
    val kinds = listOf(
        TailoringRowKind.Matching,
        TailoringRowKind.Rewriting,
        if (addsExample) TailoringRowKind.AddingExample else TailoringRowKind.CheckingMustHaves,
        TailoringRowKind.Fitting,
    )
    val stage = if (percent >= 100) kinds.size else percent / PERCENT_PER_STAGE
    return kinds.mapIndexed { index, kind ->
        TailoringRowUi(
            kind = kind,
            state = when {
                index < stage -> TmrProgressState.Done
                index == stage -> TmrProgressState.Active
                else -> TmrProgressState.Pending
            },
        )
    }
}

internal fun QuickAnswer?.addsExample(): Boolean =
    this?.choice == CHOICE_YES_REGULARLY || this?.choice == CHOICE_A_FEW_TIMES
