package com.tailormyresume.app.debug

import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.offline.ForcedWalletScenario
import com.tailormyresume.core.model.DebugScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ScenarioAccountWallet(
    private val remote: AccountWallet,
    private val offline: AccountWallet,
    private val forced: ForcedWalletScenario,
) : AccountWallet {

    private val active: AccountWallet
        get() = if (forced.scenario == DebugScenario.DEFAULT) remote else offline

    override suspend fun entitlement(): PurchaseEntitlement = active.entitlement()

    override fun observeEntitlement(): Flow<PurchaseEntitlement> = follow { it.observeEntitlement() }

    override suspend fun purchaseHistory(): List<PurchaseRecord> = active.purchaseHistory()

    override fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = follow { it.observePurchaseHistory() }

    override suspend fun clearCredits(): PurchaseEntitlement = active.clearCredits()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> follow(observe: (AccountWallet) -> Flow<T>): Flow<T> =
        forced.scenarios
            .map { it == DebugScenario.DEFAULT }
            .distinctUntilChanged()
            .flatMapLatest { useRemote -> observe(if (useRemote) remote else offline) }
}
