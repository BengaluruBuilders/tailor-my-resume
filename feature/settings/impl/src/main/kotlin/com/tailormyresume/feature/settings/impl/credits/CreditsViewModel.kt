package com.tailormyresume.feature.settings.impl.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
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
) : ViewModel() {

    val uiState: StateFlow<CreditsUiState> = combine(
        creditsRepository.observeBalance(),
        creditsRepository.observeLedger(),
        applicationRepository.observeApplications(),
    ) { credits, entries, applications ->
        CreditsUiState.Content(
            credits = credits,
            history = toHistoryRows(entries, applications, ZoneId.systemDefault()),
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
