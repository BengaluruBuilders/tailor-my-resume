package com.tailormyresume.core.domain

sealed interface SignInResult {
    data class SignedIn(val account: SignInAccount) : SignInResult

    data object AwaitingChoice : SignInResult

    data object Cancelled : SignInResult

    data class Failed(val reason: SignInFailureReason) : SignInResult
}
