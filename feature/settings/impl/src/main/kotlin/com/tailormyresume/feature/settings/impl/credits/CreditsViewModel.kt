package com.tailormyresume.feature.settings.impl.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.PaymentGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    applicationRepository: ApplicationRepository,
    private val paymentGateway: PaymentGateway,
) : ViewModel() {

    private val packs = MutableStateFlow<List<ApplicationPack>>(emptyList())

    val uiState: StateFlow<CreditsUiState> = combine(
        creditsRepository.observeBalance(),
        creditsRepository.observeLedger(),
        applicationRepository.observeApplications(),
        packs,
    ) { credits, entries, applications, packs ->
        CreditsUiState.Content(
            credits = credits,
            history = toHistoryRows(entries, applications, packs, ZoneId.systemDefault()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CreditsUiState.Loading,
    )

    init {
        viewModelScope.launch { creditsRepository.refresh() }
        viewModelScope.launch { packs.value = loadPacks() }
    }

    private suspend fun loadPacks(): List<ApplicationPack> =
        try {
            paymentGateway.packs()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            emptyList()
        }
}
