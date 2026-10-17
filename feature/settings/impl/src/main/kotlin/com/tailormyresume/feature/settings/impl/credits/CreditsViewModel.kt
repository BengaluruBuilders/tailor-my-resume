package com.tailormyresume.feature.settings.impl.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GuestTrialGateway
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
import com.tailormyresume.core.domain.account.AccountCreditBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
internal class CreditsViewModel @Inject constructor(
    private val creditsRepository: CreditsRepository,
    creditBalance: AccountCreditBalance,
    applicationRepository: ApplicationRepository,
    uids: FirebaseUidProvider = FirebaseUidProvider { null },
    private val trials: GuestTrialGateway? = null,
    private val google: com.tailormyresume.core.domain.GoogleAccountGateway? = null,
) : ViewModel() {

    private val trial = MutableStateFlow<CreditsTrialState>(CreditsTrialState.Idle)
    private var trialJob: Job? = null

    val uiState: StateFlow<CreditsUiState> = combine(
        creditBalance.observeCredits(),
        creditsRepository.observeLedger(),
        applicationRepository.observeApplications(),
        trial,
    ) { credits, entries, applications, trial ->
        CreditsUiState.Content(
            credits = credits,
            canPurchase = !uids.isAnonymous(),
            trial = trial,
            history = toHistoryRows(entries, applications, ZoneId.systemDefault()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CreditsUiState.Loading,
    )
    fun onGoogleAccount() { viewModelScope.launch { google?.open() } }

    fun onCheckTrial() {
        if (trialJob?.isActive == true) return
        trial.value = CreditsTrialState.Loading
        trialJob = viewModelScope.launch {
            trial.value = try {
                CreditsTrialState.Offer(trials?.prepare() ?: GuestTrialOffer.Unavailable())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                CreditsTrialState.Offer(GuestTrialOffer.Unavailable())
            }
        }
    }

    fun onClaimTrial() {
        val offer = (trial.value as? CreditsTrialState.Offer)?.value
        if (trialJob?.isActive == true || offer !is GuestTrialOffer.Available) return
        trial.value = CreditsTrialState.Loading
        trialJob = viewModelScope.launch {
            trial.value = try {
                when (val result = trials?.claim()) {
                    is GuestTrialResult.Refresh -> CreditsTrialState.Offer(result.offer)
                    null -> CreditsTrialState.Offer(GuestTrialOffer.Unavailable())
                    else -> CreditsTrialState.Result(result)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                CreditsTrialState.Offer(GuestTrialOffer.Unavailable())
            }
            creditsRepository.refresh()
        }
    }

    fun onResume() {
        if (trial.value != CreditsTrialState.Idle && trial.value != CreditsTrialState.Loading) onCheckTrial()
    }

    init {
        viewModelScope.launch { creditsRepository.refresh() }
    }
}
