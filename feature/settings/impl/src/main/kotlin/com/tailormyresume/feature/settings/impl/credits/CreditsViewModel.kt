package com.tailormyresume.feature.settings.impl.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    applicationRepository: ApplicationRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<CreditsUiState> = combine(
        creditsRepository.observeBalance(),
        creditsRepository.observeLedger(),
        applicationRepository.observeApplications(),
        sessionRepository.observeAccount(),
    ) { credits, entries, applications, account ->
        CreditsUiState.Content(
            credits = credits,
            history = toHistoryRows(entries, applications, ZoneId.systemDefault()),
            receiptEmail = account?.email.orEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CreditsUiState.Loading,
    )

    init {
        viewModelScope.launch { creditsRepository.refresh() }
    }
}
