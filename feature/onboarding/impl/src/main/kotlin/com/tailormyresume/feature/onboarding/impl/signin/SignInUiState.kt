package com.tailormyresume.feature.onboarding.impl.signin

import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult

internal sealed interface SignInUiState {
    data object Ready : SignInUiState
    data object SigningIn : SignInUiState
    data object Cancelled : SignInUiState
    data object PreparingGuest : SignInUiState
    data class GuestOffer(val offer: GuestTrialOffer, val busy: Boolean = false) : SignInUiState
    data class GuestResult(val result: GuestTrialResult) : SignInUiState
}
