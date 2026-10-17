package com.tailormyresume.app.credits

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

class WalletStartTask @Inject constructor(
    private val sessions: SessionRepository,
    private val wallet: WalletSource,
    private val credits: CreditsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : AppStartTask {
    override fun start() {
        scope.launch {
            sessions.observeAccount().distinctUntilChanged().collectLatest { account ->
                if (account != null) {
                    wallet.refreshOrCached()
                    credits.refresh()
                }
            }
        }
    }
}
