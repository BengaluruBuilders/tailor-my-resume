package com.tailormyresume.core.domain

import kotlinx.coroutines.flow.StateFlow

sealed interface ReferralUiState {
    data object Loading : ReferralUiState
    data class Content(
        val code: String? = null,
        val shareUrl: String? = null,
        val offerVersion: String? = null,
        val candidateSource: String = "MANUAL",
        val candidate: String? = null,
        val reward: Int? = null,
        val creditedCount: Long = 0,
        val earnedCredits: Long = 0,
        val canIssue: Boolean = false,
        val canAttach: Boolean = false,
        val guest: Boolean = false,
        val attached: Boolean = false,
        val deadline: String? = null,
        val message: ReferralMessage = ReferralMessage.None,
        val busy: Boolean = false,
        val reviewRequired: Boolean = false,
        val retryAfterSeconds: Int? = null,
    ) : ReferralUiState
}

data class ReferralEntryState(val candidate: String? = null, val reward: Int? = null, val version: String? = null, val deadline: String? = null)
data class ReferralShare(val code: String, val url: String?, val reward: Int?, val version: String?)

enum class ReferralMessage { None, Unavailable, Saved, Invalid, ReplaceRequired, Applied, PolicyChanged, Retry, ReadOnly, Conflict, Ineligible, Expired, Self, SignalChanged }

interface ReferralGateway {
    val state: StateFlow<ReferralUiState>
    val entry: StateFlow<ReferralEntryState> get() = kotlinx.coroutines.flow.MutableStateFlow(ReferralEntryState())
    val routeRequests: StateFlow<Long> get() = kotlinx.coroutines.flow.MutableStateFlow(0)
    suspend fun displayed(code: String, version: String?) = Unit
    suspend fun clearEntry() = clear()
    suspend fun share(): ReferralShare? = null
    suspend fun refresh()
    suspend fun save(code: String, replace: Boolean = false)
    suspend fun clear()
    suspend fun issue()
    suspend fun apply()
    suspend fun acceptTransfer()
}
