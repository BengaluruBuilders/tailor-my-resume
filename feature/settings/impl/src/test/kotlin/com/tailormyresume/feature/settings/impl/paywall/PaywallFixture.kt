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
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

internal class CountingPaymentGateway(
    val scripted: TestPaymentGateway = TestPaymentGateway(),
    var purchaseGate: CompletableDeferred<Unit>? = null,
    var restoreGate: CompletableDeferred<Unit>? = null,
    var packsFailures: Int = 0,
    var restoreFailure: Exception? = null,
) : PaymentGateway by scripted {
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
        return scripted.restorePurchases()
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
        refreshCalls += 1
        refreshGate?.await()
        balance.value = serverBalance
    }

    override suspend fun clear() = Unit

    override suspend fun removeForApplication(applicationId: String) = Unit
}
