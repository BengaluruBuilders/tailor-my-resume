package com.tailormyresume.feature.settings.impl.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.GoogleAccountGateway
import com.tailormyresume.core.domain.ReferralGateway
import com.tailormyresume.core.domain.ReferralUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReferralsViewModel @Inject constructor(private val referrals: ReferralGateway, private val google: GoogleAccountGateway) : ViewModel() {
    val uiState: StateFlow<ReferralUiState> = referrals.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReferralUiState.Loading)
    private var job: Job? = null
    private fun act(action: suspend () -> Unit) {
        if (job?.isActive == true) return
        job = viewModelScope.launch { action() }
    }
    fun refresh() = act { referrals.refresh() }
    fun save(code: String, replace: Boolean = false) = act { referrals.save(code, replace) }
    fun clear() = act { referrals.clear() }
    fun issue() = act { referrals.issue() }
    fun apply() = act { referrals.apply() }
    fun review() = act { referrals.acceptTransfer() }
    fun google() = act { google.open() }
    val routeRequests = referrals.routeRequests
    fun displayed(code: String, version: String?) { viewModelScope.launch { referrals.displayed(code, version) } }
    suspend fun share() = referrals.share()
}
