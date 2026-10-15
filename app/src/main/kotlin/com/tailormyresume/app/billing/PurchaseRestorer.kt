package com.tailormyresume.app.billing

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.PaymentGateway
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRestorer @Inject constructor(
    private val payments: Lazy<PaymentGateway>,
    private val billing: Lazy<PlayBilling>,
    private val sessionRepository: SessionRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : AppStartTask {

    override fun start() {
        scope.launch {
            sessionRepository.observeAccount().map { it?.id }.distinctUntilChanged().filterNotNull().collect { restore() }
        }
        scope.launch { billing.get().unsolicitedPurchases.collect { restore() } }
    }

    private suspend fun restore() {
        try {
            payments.get().restorePurchases()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return
        }
    }
}
