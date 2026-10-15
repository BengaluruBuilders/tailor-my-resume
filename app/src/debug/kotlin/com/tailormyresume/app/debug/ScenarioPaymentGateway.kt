package com.tailormyresume.app.debug

import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.domain.offline.ForcedPaymentScenario
import com.tailormyresume.core.model.DebugScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ScenarioPaymentGateway(
    private val remote: PaymentGateway,
    private val offline: PaymentGateway,
    private val forced: ForcedPaymentScenario,
) : PaymentGateway {

    private val active: PaymentGateway
        get() = if (forced.scenario == DebugScenario.DEFAULT) remote else offline

    override suspend fun packs(): List<ApplicationPack> = active.packs()

    override suspend fun purchase(packId: String): PurchaseResult = active.purchase(packId)

    override suspend fun entitlement(): PurchaseEntitlement = active.entitlement()

    override fun observeEntitlement(): Flow<PurchaseEntitlement> = follow { it.observeEntitlement() }

    override suspend fun purchaseHistory(): List<PurchaseRecord> = active.purchaseHistory()

    override fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = follow { it.observePurchaseHistory() }

    override suspend fun restorePurchases(): PurchaseEntitlement = active.restorePurchases()

    override suspend fun unlock(applicationId: String): CreditSpend = active.unlock(applicationId)

    override suspend fun clearCredits(): PurchaseEntitlement = active.clearCredits()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> follow(observe: (PaymentGateway) -> Flow<T>): Flow<T> =
        forced.scenarios
            .map { it == DebugScenario.DEFAULT }
            .distinctUntilChanged()
            .flatMapLatest { useRemote -> observe(if (useRemote) remote else offline) }
}
