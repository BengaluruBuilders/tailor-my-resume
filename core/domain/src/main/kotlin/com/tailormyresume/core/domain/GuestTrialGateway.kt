package com.tailormyresume.core.domain

sealed interface GuestTrialOffer {
    data class Available(val credits: Int, val changed: Boolean = false) : GuestTrialOffer
    data object Existing : GuestTrialOffer
    data object NotEligible : GuestTrialOffer
    data class Unavailable(val retryAfterSeconds: Int? = null, val canContinue: Boolean = true) : GuestTrialOffer
}

sealed interface GuestTrialResult {
    data class Granted(val credits: Int) : GuestTrialResult
    data object Existing : GuestTrialResult
    data object NotEligible : GuestTrialResult
    data class Refresh(val offer: GuestTrialOffer) : GuestTrialResult
}

interface GuestTrialGateway {
    suspend fun prepare(): GuestTrialOffer
    suspend fun claim(): GuestTrialResult
    suspend fun continueAsGuest()
}
