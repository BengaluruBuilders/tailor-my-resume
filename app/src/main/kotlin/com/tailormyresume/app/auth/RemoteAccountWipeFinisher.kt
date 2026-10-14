package com.tailormyresume.app.auth

import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import javax.inject.Inject

class RemoteAccountWipeFinisher @Inject constructor(
    private val signInGateway: SignInGateway,
    private val wiper: LocalDataWiper,
    private val pendingWipe: PendingAccountWipe = PendingAccountWipe.None,
) : AccountWipeFinisher {
    override suspend fun finish() {
        val currentId = signInGateway.currentAccount()?.id
        val markerId = pendingWipe.uid()
        if (currentId != null && markerId != null && currentId != markerId) {
            pendingWipe.clear()
            return
        }
        signInGateway.signOut()
        wiper.wipeAll()
    }
}
