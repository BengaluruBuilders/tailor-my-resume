package com.tailormyresume.app.billing

import com.tailormyresume.app.auth.AccountOperationGuard
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseState
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

@Singleton
class RemoteAccountWallet @Inject constructor(
    private val api: TailorMyResumeApi,
    private val wallet: WalletSource,
    private val uids: FirebaseUidProvider,
    private val guard: AccountOperationGuard = AccountOperationGuard(uids, LocalWorkFence()),
) : AccountWallet {
    private class OwnedHistory(val ticket: AccountOperationGuard.Ticket, val generation: Int, val records: List<PurchaseRecord>)
    private val lock = Any()
    private var generation = 0
    private var history: OwnedHistory? = null

    override suspend fun entitlement(): PurchaseEntitlement {
        val ticket = guard.capture()
        return wallet.refresh().toEntitlement(emptyList()).also { guard.check(ticket) }
    }

    override fun observeEntitlement(): Flow<PurchaseEntitlement> = wallet.wallet.map { current ->
        current?.toEntitlement(emptyList()) ?: NO_CREDITS.copy(hasKnownBalance = false)
    }.onStart {
        emit(wallet.cached?.toEntitlement(emptyList()) ?: NO_CREDITS.copy(hasKnownBalance = false))
        wallet.refreshOrCached()
    }

    override suspend fun purchaseHistory(): List<PurchaseRecord> {
        val ticket = guard.capture()
        val started = synchronized(lock) { generation }
        val records = apiResult { api.purchases().purchases }.getOrThrow().map { purchase ->
            PurchaseRecord(purchase.productId, purchase.orderId, Instant.parse(purchase.purchasedAt), PurchaseState.COMPLETED)
        }
        guard.check(ticket)
        synchronized(lock) {
            if (started != generation) throw CancellationException("Account history changed")
            history = OwnedHistory(ticket, started, records)
        }
        return records
    }

    override fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = flow {
        val cached = synchronized(lock) { history }?.takeIf { held ->
            runCatching { guard.check(held.ticket) }.isSuccess && held.generation == synchronized(lock) { generation }
        }?.records
        cached?.let { emit(it) }
        val fresh = try {
            purchaseHistory()
        } catch (failure: Exception) {
            if (failure is CancellationException || cached == null) throw failure
            return@flow
        }
        emit(fresh)
    }

    override suspend fun clearCredits(): PurchaseEntitlement {
        synchronized(lock) {
            generation++
            history = null
            wallet.clear()
        }
        return NO_CREDITS
    }
}
