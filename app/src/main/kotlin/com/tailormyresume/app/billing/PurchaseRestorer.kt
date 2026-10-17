package com.tailormyresume.app.billing

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.model.AccountMode
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
            sessionRepository.observeAccount().distinctUntilChanged().collectLatest { account ->
                if (account?.mode == AccountMode.GOOGLE) {
                    coroutineScope {
                        launch { restore() }
                        billing.get().unsolicitedPurchases.collect { restore() }
                    }
                }
            }
        }
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
