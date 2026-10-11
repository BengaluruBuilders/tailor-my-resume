package com.tailormyresume.feature.settings.impl.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
internal class PaywallViewModel @Inject constructor(
    private val paymentGateway: PaymentGateway,
    private val creditsRepository: CreditsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val packs = MutableStateFlow<List<ApplicationPack>>(emptyList())
    private val chosenPackId = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val eventChannel = Channel<PaywallEvent>(Channel.BUFFERED)

    val events: Flow<PaywallEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<PaywallUiState> = combine(
        creditsRepository.observeKnownBalance(),
        packs,
        chosenPackId,
        busy,
    ) { credits, packs, chosen, busy ->
        PaywallUiState(
            credits = credits,
            packs = packs,
            selectedPackId = selectedPack(packs, chosen)?.id,
            busy = busy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaywallUiState.Initial,
    )

    init {
        viewModelScope.launch { creditsRepository.refresh() }
        viewModelScope.launch { loadPacks() }
    }

    fun onSelectPack(packId: String) {
        chosenPackId.value = packId
    }

    fun onBuy() {
        if (!busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val pack = selectedPack(packs.value, chosenPackId.value)
                if (pack == null) loadPacks() else purchase(pack)
            } finally {
                busy.value = false
            }
        }
    }

    fun onRestore() {
        if (!busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val added = restoredCredits()
                eventChannel.send(if (added > 0) PaywallEvent.Added(added) else PaywallEvent.NothingToRestore)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                eventChannel.send(PaywallEvent.RestoreFailed)
            } finally {
                busy.value = false
            }
        }
    }

    private suspend fun restoredCredits(): Int {
        check(creditsRepository.refreshConfirmed()) { "balance before restore unavailable" }
        val before = creditsRepository.observeBalance().first()
        paymentGateway.restorePurchases()
        check(creditsRepository.refreshConfirmed()) { "balance after restore unavailable" }
        return creditsRepository.observeBalance().first() - before
    }

    private suspend fun loadPacks() {
        val loaded = try {
            paymentGateway.packs()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            emptyList()
        }
        if (loaded.isEmpty()) eventChannel.send(PaywallEvent.PricesUnavailable) else packs.value = loaded
    }

    private suspend fun purchase(pack: ApplicationPack) {
        val result = try {
            paymentGateway.purchase(pack.id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            eventChannel.send(PaywallEvent.PaymentFailed)
            return
        }
        when (result) {
            is PurchaseResult.Completed -> credit(pack)
            is PurchaseResult.Pending -> eventChannel.send(PaywallEvent.Pending)
            is PurchaseResult.Failed -> eventChannel.send(
                if (result.reason == PurchaseFailureReason.PaymentUnconfirmed) PaywallEvent.PaymentUnconfirmed else PaywallEvent.PaymentFailed,
            )
            PurchaseResult.Cancelled -> Unit
        }
    }

    private suspend fun credit(pack: ApplicationPack) {
        withContext(NonCancellable) {
            creditsRepository.record(
                CreditLedgerEntry(
                    kind = CreditLedgerKind.PURCHASE,
                    amount = pack.credits,
                    applicationId = null,
                    productId = pack.id,
                    createdAt = clock.now(),
                ),
            )
        }
        creditsRepository.refresh()
        eventChannel.send(PaywallEvent.Added(pack.credits))
    }
}

private fun selectedPack(packs: List<ApplicationPack>, chosenId: String?): ApplicationPack? =
    packs.firstOrNull { it.id == chosenId }
        ?: packs.firstOrNull { it.id == ApplicationPack.APPLICATION_PACK_FIFTEEN }
        ?: packs.firstOrNull()
