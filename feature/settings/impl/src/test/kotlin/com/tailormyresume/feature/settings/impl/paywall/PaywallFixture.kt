package com.tailormyresume.feature.settings.impl.paywall

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

internal class CountingPaymentGateway(
    val scripted: TestPaymentGateway = TestPaymentGateway().withFreeCredits(0),
    var purchaseGate: CompletableDeferred<Unit>? = null,
    var restoreGate: CompletableDeferred<Unit>? = null,
    var packsFailures: Int = 0,
    var restoreFailure: Exception? = null,
    initialEntitlement: PurchaseEntitlement? = null,
    var restoredEntitlement: PurchaseEntitlement? = null,
    var entitlementFailure: Exception? = null,
) : PaymentGateway by scripted {
    private val held = MutableStateFlow(initialEntitlement)

    override suspend fun entitlement(): PurchaseEntitlement {
        entitlementFailure?.let { throw it }
        return held.value ?: scripted.entitlement()
    }

    override fun observeEntitlement(): Flow<PurchaseEntitlement> = combine(scripted.observeEntitlement(), held) { stored, restored -> restored ?: stored }

    var packsCalls = 0
    var purchaseCalls = 0
    var restoreCalls = 0
    val purchasedPackIds = mutableListOf<String>()

    override suspend fun packs(): List<ApplicationPack> {
        packsCalls += 1
        if (packsCalls <= packsFailures) throw IOException("offline")
        return scripted.packs()
    }

    override suspend fun purchase(packId: String): PurchaseResult {
        purchaseCalls += 1
        purchasedPackIds += packId
        purchaseGate?.await()
        return scripted.purchase(packId)
    }

    override suspend fun restorePurchases(): PurchaseEntitlement {
        restoreCalls += 1
        restoreGate?.await()
        restoreFailure?.let { throw it }
        return (restoredEntitlement ?: scripted.restorePurchases()).also { held.value = it }
    }
}

internal class RecordingCreditsRepository(
    private val delegate: TestCreditsRepository = TestCreditsRepository(),
) : CreditsRepository by delegate {
    val calls = mutableListOf<String>()
    val recorded = mutableListOf<CreditLedgerEntry>()

    override suspend fun record(entry: CreditLedgerEntry) {
        calls += "record"
        recorded += entry
        delegate.record(entry)
    }

    override suspend fun refresh() {
        calls += "refresh"
        delegate.refresh()
    }
}

internal class RemoteStyleCreditsRepository(
    initialBalance: Int,
    var serverBalance: Int = initialBalance,
    var refreshGate: CompletableDeferred<Unit>? = null,
    var failingRefreshes: Set<Int> = emptySet(),
) : CreditsRepository {
    private val balance = MutableStateFlow(initialBalance)
    var recordCalls = 0
    var refreshCalls = 0

    override fun observeBalance(): Flow<Int> = balance

    override fun observeLedger(): Flow<List<CreditLedgerEntry>> = flowOf(emptyList())

    override suspend fun record(entry: CreditLedgerEntry) {
        recordCalls += 1
    }

    override suspend fun refresh() {
        refreshConfirmed()
    }

    override suspend fun refreshConfirmed(): Boolean {
        refreshCalls += 1
        refreshGate?.await()
        if (refreshCalls in failingRefreshes) return false
        balance.value = serverBalance
        return true
    }

    override suspend fun clear() = Unit

    override suspend fun removeForApplication(applicationId: String) = Unit
}
