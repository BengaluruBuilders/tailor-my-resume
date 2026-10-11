package com.tailormyresume.feature.settings.impl.credits

internal sealed interface CreditsUiState {
    data object Loading : CreditsUiState

    data class Content(val credits: Int?, val history: List<CreditsHistoryRow>) : CreditsUiState
}
