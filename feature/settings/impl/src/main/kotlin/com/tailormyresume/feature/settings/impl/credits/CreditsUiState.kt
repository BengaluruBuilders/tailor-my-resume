package com.tailormyresume.feature.settings.impl.credits

import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult

internal sealed interface CreditsUiState {
    data object Loading : CreditsUiState

    data class Content(val credits: Int?, val history: List<CreditsHistoryRow>, val isGuest: Boolean = false, val trial: CreditsTrialState = CreditsTrialState.Idle) : CreditsUiState
}

internal sealed interface CreditsTrialState {
    data object Idle : CreditsTrialState
    data object Loading : CreditsTrialState
    data class Offer(val value: GuestTrialOffer) : CreditsTrialState
    data class Result(val value: GuestTrialResult) : CreditsTrialState
}
