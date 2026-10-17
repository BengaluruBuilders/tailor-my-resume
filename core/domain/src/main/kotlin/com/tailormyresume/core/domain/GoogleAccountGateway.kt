package com.tailormyresume.core.domain

import kotlinx.coroutines.flow.StateFlow

sealed interface GoogleAccountState {
    data object Idle : GoogleAccountState
    data object Busy : GoogleAccountState
    data object SaveEdits : GoogleAccountState
    data class Offer(val signup: Int?, val base: Int?, val canClaim: Boolean, val changed: Boolean = false, val google: Boolean = true, val retryAfterSeconds: Int? = null) : GoogleAccountState
    data class Conflict(val hasProfile: Boolean, val applications: List<Pair<String, String>>) : GoogleAccountState
    data class ProfileChoice(val guestName: String, val destinationName: String) : GoogleAccountState
    data class Retry(val canCancel: Boolean, val retryAfterSeconds: Int? = null) : GoogleAccountState
    data class Complete(val signup: Int?, val base: Int?, val preservedWork: Boolean = false, val retryAfterSeconds: Int? = null) : GoogleAccountState
}

interface GoogleAccountGateway {
    val state: StateFlow<GoogleAccountState>
    suspend fun signIn(): SignInResult
    suspend fun open()
    suspend fun claim()
    suspend fun continueConflict(useProfile: Boolean, applications: Set<String>)
    suspend fun chooseProfile(useGuest: Boolean)
    suspend fun retry()
    suspend fun cancel()
    suspend fun dismiss()
}
