package com.tailormyresume.app.auth

import com.tailormyresume.core.data.account.LocalWorkCheckpoint
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AccountPrivateWorkEraser @Inject constructor(
    private val uids: FirebaseUidProvider,
    private val sessions: SessionRepository,
    private val pending: PendingAccountWipe,
    private val checkpoints: LocalWorkCheckpoint,
    private val journals: PrivateUpgradeStore,
    private val config: TailorMyResumeApiConfig,
    private val fence: LocalWorkFence,
    private val referrals: com.tailormyresume.app.referrals.PrivateReferralStore? = null,
) {
    suspend fun erase() {
        val owner = pending.uid() ?: uids.uid() ?: sessions.observeAccount().first()?.id ?: sessions.lastAccountId() ?: return
        fence.freezeForErasure()
        referrals?.eraseFor(owner, config.baseUrl)
        checkpoints.erase(owner, config.baseUrl)
        journals.eraseFor(owner, config.baseUrl)
    }
}
