package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.observeValue
import com.tailormyresume.core.data.mock.readValue
import com.tailormyresume.core.data.mock.writeValue
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.IdGenerator
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.model.CreditKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class OfflineAccountWallet @Inject constructor(
    private val store: MockStateStore,
    private val latency: MockLatency,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val forced: ForcedWalletScenario = ForcedWalletScenario(),
) : AccountWallet {
    private val mutex = Mutex()
    private var startingFreeCredits = 1
    fun withFreeCredits(credits: Int): OfflineAccountWallet = apply { startingFreeCredits = credits }

    suspend fun withHistoricalPurchase(packId: String, credits: Int = 5) = mutex.withLock {
        val state = load()
        save(state.copy(purchasedCreditAmount = state.purchasedCredits + credits).confirm(packId, "mock-order-" + idGenerator.newId(), clock.now().toEpochMilliseconds()))
        Unit
    }

    override suspend fun entitlement(): PurchaseEntitlement = mutex.withLock { load().toEntitlement() }
    override fun observeEntitlement(): Flow<PurchaseEntitlement> =
        observeState().map(PaymentState::toEntitlement).distinctUntilChanged()
    override suspend fun purchaseHistory(): List<PurchaseRecord> = mutex.withLock { load().history() }
    override fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = observeState().map { it.history() }.distinctUntilChanged()
    override suspend fun clearCredits(): PurchaseEntitlement = mutex.withLock {
        save(PaymentState(freeCredits = 0, closed = true)).toEntitlement()
    }

    suspend fun spendFixtureCredit(applicationId: String): CreditSpend = mutex.withLock {
        val state = load()
        val unlocked = state.copy(unlockedApplicationIds = state.unlockedApplicationIds + applicationId)
        when {
            applicationId in state.unlockedApplicationIds -> CreditSpend.Spent(state.toEntitlement(), null)
            state.freeCredits > 0 -> CreditSpend.Spent(save(unlocked.copy(freeCredits = state.freeCredits - 1)).toEntitlement(), CreditKind.FREE)
            state.purchasedCredits > 0 -> CreditSpend.Spent(save(unlocked.copy(purchasedCreditAmount = state.purchasedCredits - 1, spentPurchasedCredits = 0, confirmedPackIds = emptyList())).toEntitlement(), CreditKind.PURCHASED)
            else -> CreditSpend.NoCreditLeft
        }
    }

    private fun PaymentState.history(): List<PurchaseRecord> = purchases.map { it.toModel() }.reversed()
    private fun observeState(): Flow<PaymentState> = store.observeValue(PAYMENT_STATE_KEY, PaymentState.serializer()).map { it ?: PaymentState(startingFreeCredits) }
    private suspend fun load(): PaymentState = store.readValue(PAYMENT_STATE_KEY, PaymentState.serializer()) ?: PaymentState(startingFreeCredits)
    private suspend fun save(state: PaymentState): PaymentState {
        store.writeValue(PAYMENT_STATE_KEY, PaymentState.serializer(), state)
        return state
    }
}
