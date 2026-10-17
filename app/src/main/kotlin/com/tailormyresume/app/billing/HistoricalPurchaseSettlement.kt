package com.tailormyresume.app.billing

import com.tailormyresume.app.auth.AccountOperationGuard
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.PurchaseRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricalPurchaseSettlement @Inject constructor(
    private val api: TailorMyResumeApi,
    private val wallet: WalletSource,
    private val billing: PlayBilling,
    private val uids: FirebaseUidProvider,
    private val sessions: SessionRepository,
    private val firebase: FirebaseSessionClient,
    private val guard: AccountOperationGuard,
    @ApplicationScope private val scope: CoroutineScope,
    private val credits: CreditsRepository,
) {
    private data class Actor(val ticket: AccountOperationGuard.Ticket, val generation: Int)
    private val mutex = Mutex()
    private val reposts = mutableMapOf<String, Job>()
    private var generation = 0

    suspend fun reconcile(completion: PlayPurchase? = null) = mutex.withLock {
        val actor = Actor(guard.capture(), synchronized(reposts) { generation })
        checkOwner(actor)
        val owned = billing.ownedPurchases()
        checkOwner(actor)
        (listOfNotNull(completion) + owned).distinctBy { it.token }.forEach { purchase ->
            if (purchase.state == PlayPurchaseState.PURCHASED && !post(purchase, actor)) retry(purchase, actor)
        }
    }

    fun invalidate() = synchronized(reposts) {
        generation++
        reposts.values.forEach(Job::cancel)
        reposts.clear()
    }

    internal fun repostCount(): Int = synchronized(reposts) { reposts.size }

    private suspend fun checkOwner(actor: Actor) {
        currentCoroutineContext().ensureActive()
        guard.check(actor.ticket)
        if (actor.generation != synchronized(reposts) { generation }) throw CancellationException("Settlement session changed")
        val account = sessions.observeAccount().first()
        val verified = firebase.currentUser()
        guard.check(actor.ticket)
        if (account?.mode != AccountMode.GOOGLE || account.id != actor.ticket.owner || uids.isAnonymous() ||
            verified?.uid != account.id || verified.mode != AccountMode.GOOGLE || verified.googleSubject.isNullOrBlank()
        ) throw CancellationException("Settlement needs the current Google account")
    }

    private suspend fun post(purchase: PlayPurchase, actor: Actor): Boolean {
        checkOwner(actor)
        val started = wallet.generation()
        val response = apiResult { api.purchase(PurchaseRequest(purchase.productId, purchase.token)) }
        checkOwner(actor)
        response.onSuccess { result ->
            if (wallet.update(result.wallet, started)) credits.refresh()
        }
        return response.isSuccess || (response.exceptionOrNull() as? ApiException)?.error in REJECTIONS
    }

    private fun retry(purchase: PlayPurchase, actor: Actor) = synchronized(reposts) {
        if (actor.generation != generation || reposts[purchase.token]?.isActive == true) return@synchronized
        val job = scope.launch(start = CoroutineStart.LAZY) {
            for (wait in BACKOFF_MILLIS) {
                delay(wait)
                if (post(purchase, actor)) return@launch
            }
        }
        reposts[purchase.token] = job
        job.invokeOnCompletion { synchronized(reposts) { reposts.remove(purchase.token, job) } }
        job.start()
    }

    private companion object {
        val BACKOFF_MILLIS = listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L)
        val REJECTIONS = setOf(ApiError.PurchaseInvalid, ApiError.Forbidden, ApiError.GoogleRequired)
    }
}
