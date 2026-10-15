package com.tailormyresume.app.auth

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.domain.account.FinishPendingAccountWipeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class PendingWipeStartTask @Inject constructor(
    private val finishPendingWipe: FinishPendingAccountWipeUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) : AppStartTask {
    override fun start() {
        scope.launch { finishPendingWipe() }
    }
}
