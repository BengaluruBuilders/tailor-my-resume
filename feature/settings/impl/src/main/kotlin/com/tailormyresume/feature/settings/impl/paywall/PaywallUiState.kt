package com.tailormyresume.feature.settings.impl.paywall

import com.tailormyresume.core.domain.ApplicationPack

internal data class PaywallUiState(
    val credits: Int?,
    val packs: List<ApplicationPack>,
    val selectedPackId: String?,
    val busy: Boolean,
) {
    val selectedPack: ApplicationPack?
        get() = packs.firstOrNull { it.id == selectedPackId }

    companion object {
        val Initial = PaywallUiState(credits = null, packs = emptyList(), selectedPackId = null, busy = false)
    }
}

internal sealed interface PaywallEvent {
    data class Added(val credits: Int) : PaywallEvent

    data object Pending : PaywallEvent

    data object PaymentFailed : PaywallEvent

    data object PaymentUnconfirmed : PaywallEvent

    data object NothingToRestore : PaywallEvent

    data object RestoreFailed : PaywallEvent

    data object PricesUnavailable : PaywallEvent
}
