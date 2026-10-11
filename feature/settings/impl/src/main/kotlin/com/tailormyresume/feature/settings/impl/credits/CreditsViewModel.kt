package com.tailormyresume.feature.settings.impl.credits

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.PaymentGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class CreditsViewModel @Inject constructor(
    private val creditsRepository: CreditsRepository,
    private val applicationRepository: ApplicationRepository,
    private val paymentGateway: PaymentGateway,
) : ViewModel() {

    val uiState: StateFlow<CreditsUiState> = MutableStateFlow(CreditsUiState.Loading)
}
