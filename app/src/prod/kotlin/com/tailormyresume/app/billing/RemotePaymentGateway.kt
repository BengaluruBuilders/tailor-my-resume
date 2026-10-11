package com.tailormyresume.app.billing

import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.domain.PurchaseState
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.PurchaseRequest
import com.tailormyresume.core.network.dto.PurchaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

@Singleton
class RemotePaymentGateway @Inject constructor(
    private val api: TailorMyResumeApi,
    private val wallet: WalletSource,
    private val billing: PlayBilling,
    private val uids: FirebaseUidProvider,
    @ApplicationScope private val scope: CoroutineScope,
    private val credits: CreditsRepository,
) : PaymentGateway {
    private val pending = MutableStateFlow<List<String>>(emptyList())
    private val reposts = mutableMapOf<String, Job>()
    private var generation = 0

    @Volatile
    private var listedProductIds: Set<String>? = null

    private class OwnedHistory(val uid: String?, val records: List<PurchaseRecord>)

    @Volatile
    private var history: OwnedHistory? = null

    private fun cachedHistory(): List<PurchaseRecord>? = history?.takeIf { it.uid == uids.uid() }?.records

    internal fun repostCount() = synchronized(reposts) { reposts.size }

    override suspend fun packs(): List<ApplicationPack> {
        val listed = apiResult { api.packs().packs }.getOrThrow()
        listedProductIds = listed.map { it.productId }.toSet()
        return listed.mapNotNull { pack ->
            billing.productDetails(pack.productId)?.let { product ->
                ApplicationPack(
                    id = pack.productId,
                    name = product.name,
                    credits = pack.credits,
                    priceInPaise = product.priceMicros / MICROS_PER_PAISE,
                    currencyCode = product.currencyCode,
                    creditsExpire = pack.creditsExpire,
                )
            }
        }
    }

    override suspend fun entitlement(): PurchaseEntitlement = wallet.refresh().toEntitlement(pending.value)

    override fun observeEntitlement(): Flow<PurchaseEntitlement> =
        combine(wallet.wallet, pending) { current, held ->
            current?.toEntitlement(held) ?: NO_CREDITS.copy(pendingPackIds = held)
        }
            .onStart {
                emit(current())
                wallet.refreshOrCached()
            }

    override suspend fun purchaseHistory(): List<PurchaseRecord> {
        val epoch = currentGeneration()
        val uid = uids.uid()
        return fetchHistory().also { fresh -> keepHistory(fresh, epoch, uid) }
    }

    override fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = flow {
        val cached = cachedHistory()
        cached?.let { emit(it) }
        val epoch = currentGeneration()
        val uid = uids.uid()
        val fresh = runCatching { fetchHistory() }.getOrElse { failure ->
            if (failure is CancellationException || cached == null) throw failure
            return@flow
        }
        if (keepHistory(fresh, epoch, uid)) emit(fresh)
    }

    private suspend fun fetchHistory(): List<PurchaseRecord> =
        apiResult { api.purchases().purchases }.getOrThrow().map { purchase ->
            PurchaseRecord(purchase.productId, purchase.orderId, Instant.parse(purchase.purchasedAt), PurchaseState.COMPLETED)
        }

    private fun keepHistory(fresh: List<PurchaseRecord>, epoch: Int, uid: String?): Boolean = synchronized(reposts) {
        (epoch == generation && uid == uids.uid()).also { current -> if (current) history = OwnedHistory(uid, fresh) }
    }

    override suspend fun purchase(packId: String): PurchaseResult {
        val epoch = currentGeneration()
        val uid = uids.uid() ?: return failed(PurchaseFailureReason.PurchaseUnavailable)
        if (listedProductIds?.contains(packId) == false) return failed(PurchaseFailureReason.PurchaseUnavailable)
        return when (val outcome = billing.launchPurchase(packId, obfuscatedAccountId(uid))) {
            PlayPurchaseResult.Cancelled -> PurchaseResult.Cancelled
            PlayPurchaseResult.Failed -> failed(PurchaseFailureReason.PaymentUnavailable)
            PlayPurchaseResult.AlreadyOwned -> settleOwned(packId, epoch)
            is PlayPurchaseResult.Done -> settle(outcome.purchase, SETTLE_RETRIES, epoch)
        }
    }

    override suspend fun restorePurchases(): PurchaseEntitlement {
        val epoch = currentGeneration()
        billing.ownedPurchases().forEach { owned -> settle(owned, retries = 0, epoch = epoch) }
        return entitlement()
    }

    override suspend fun unlock(applicationId: String): CreditSpend = CreditSpend.Spent(current(), kind = null)

    override suspend fun clearCredits(): PurchaseEntitlement {
        synchronized(reposts) {
            generation++
            reposts.values.forEach(Job::cancel)
            reposts.clear()
            wallet.clear()
            history = null
            pending.value = emptyList()
        }
        return NO_CREDITS
    }

    private fun currentGeneration() = synchronized(reposts) { generation }

    private suspend fun settleOwned(packId: String, epoch: Int): PurchaseResult {
        val owned = billing.ownedPurchases().firstOrNull { it.productId == packId }
            ?: return failed(PurchaseFailureReason.PaymentUnconfirmed)
        return settle(owned, SETTLE_RETRIES, epoch)
    }

    private suspend fun settle(purchase: PlayPurchase, retries: Int, epoch: Int): PurchaseResult {
        if (purchase.state == PlayPurchaseState.PENDING) return hold(purchase.productId, epoch)
        return try {
            post(purchase, retries, epoch)
        } catch (cancellation: CancellationException) {
            hold(purchase.productId, epoch)
            repostInBackground(purchase, epoch)
            throw cancellation
        }
    }

    private suspend fun post(purchase: PlayPurchase, retries: Int, epoch: Int): PurchaseResult {
        if (currentGeneration() != epoch) return failed(PurchaseFailureReason.PaymentUnconfirmed)
        val started = wallet.generation()
        val result = apiResult { api.purchase(PurchaseRequest(purchase.productId, purchase.token)) }
        val response = result.getOrElse { failure ->
            if (failure.isRejection()) return unconfirmed(purchase.productId, epoch)
            val held = hold(purchase.productId, epoch)
            if (retries == 0 || (failure as? ApiException)?.error == ApiError.PurchasePending) {
                repostInBackground(purchase, epoch)
                return held
            }
            delay(RETRY_DELAY_MILLIS * (SETTLE_RETRIES - retries + 1))
            return post(purchase, retries - 1, epoch)
        }
        return recorded(purchase.productId, response, epoch, started)
    }

    private fun repostInBackground(purchase: PlayPurchase, epoch: Int) = synchronized(reposts) {
        if (epoch != generation || reposts[purchase.token]?.isActive == true) return@synchronized
        val job = scope.launch(start = CoroutineStart.LAZY) { repost(purchase, epoch) }
        reposts[purchase.token] = job
        job.invokeOnCompletion { synchronized(reposts) { reposts.remove(purchase.token, job) } }
        job.start()
    }

    private suspend fun repost(purchase: PlayPurchase, epoch: Int) {
        for (wait in REPOST_BACKOFF_MILLIS) {
            delay(wait)
            if (purchase.productId !in pending.value) return
            val started = wallet.generation()
            val result = apiResult { api.purchase(PurchaseRequest(purchase.productId, purchase.token)) }
            result.onSuccess {
                recorded(purchase.productId, it, epoch, started)
                return
            }
            if (result.exceptionOrNull()?.isRejection() == true) {
                unconfirmed(purchase.productId, epoch)
                return
            }
        }
    }

    private fun recorded(productId: String, response: PurchaseResponse, epoch: Int, started: Int): PurchaseResult {
        val result = synchronized(reposts) {
            if (epoch != generation || !wallet.update(response.wallet, started)) {
                failed(PurchaseFailureReason.PaymentUnconfirmed)
            } else {
                pending.update { it - productId }
                PurchaseResult.Completed(response.wallet.toEntitlement(pending.value))
            }
        }
        if (result is PurchaseResult.Completed) scope.launch { credits.refresh() }
        return result
    }

    private fun unconfirmed(productId: String, epoch: Int): PurchaseResult {
        synchronized(reposts) {
            if (epoch == generation) pending.update { it - productId }
        }
        return failed(PurchaseFailureReason.PaymentUnconfirmed)
    }

    private fun Throwable.isRejection() = (this as? ApiException)?.error in REJECTIONS

    private fun hold(productId: String, epoch: Int): PurchaseResult {
        synchronized(reposts) {
            if (epoch == generation) pending.update { if (productId in it) it else it + productId }
        }
        return PurchaseResult.Pending(current())
    }

    private fun failed(reason: PurchaseFailureReason) = PurchaseResult.Failed(reason, current())

    private fun current() = wallet.cached?.toEntitlement(pending.value) ?: NO_CREDITS.copy(pendingPackIds = pending.value)

    private companion object {
        const val MICROS_PER_PAISE = 10_000L
        const val SETTLE_RETRIES = 2
        val REJECTIONS = setOf(ApiError.PurchaseInvalid, ApiError.Forbidden)
        const val RETRY_DELAY_MILLIS = 1_000L
        val REPOST_BACKOFF_MILLIS = listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L)
    }
}
