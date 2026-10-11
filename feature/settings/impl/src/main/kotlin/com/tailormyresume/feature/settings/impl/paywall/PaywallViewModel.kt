package com.tailormyresume.feature.settings.impl.paywall

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.PaymentGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
internal class PaywallViewModel @Inject constructor(
    private val paymentGateway: PaymentGateway,
    private val creditsRepository: CreditsRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<PaywallUiState> = MutableStateFlow(PaywallUiState.Initial)

    val events: Flow<PaywallEvent> = emptyFlow()

    fun onSelectPack(packId: String) = Unit

    fun onBuy() = Unit

    fun onRestore() = Unit
}
