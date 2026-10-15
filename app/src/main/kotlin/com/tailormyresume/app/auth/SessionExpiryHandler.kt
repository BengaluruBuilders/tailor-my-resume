package com.tailormyresume.app.auth

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.network.SessionExpiredListener
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiryHandler @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val gateway: Lazy<SignInGateway>,
    private val uidProvider: FirebaseUidProvider,
    private val config: FirebaseConfig,
    @ApplicationScope private val scope: CoroutineScope,
    private val pendingWipe: PendingAccountWipe = PendingAccountWipe.None,
) : SessionExpiredListener, AppStartTask {
    private val signingOut = AtomicBoolean(false)
    private val signOutMutex = Mutex()

    fun onSessionExpired() = onSessionExpired(accountGone = false)

    override fun onSessionExpired(accountGone: Boolean) {
        if (accountGone) {
            scope.launch { signOutLocked(promoteMarker = true) }
            return
        }
        if (!signingOut.compareAndSet(false, true)) return
        scope.launch {
            try {
                signOutLocked(promoteMarker = false)
            } finally {
                signingOut.set(false)
            }
        }
    }

    private suspend fun signOutLocked(promoteMarker: Boolean) = signOutMutex.withLock {
        val accountId = sessionRepository.observeAccount().first()?.id
        if (promoteMarker) {
            val markerOwner = accountId ?: sessionRepository.lastAccountId()
            if (markerOwner != null) pendingWipe.promoteToServerClosed(markerOwner)
        }
        if (accountId != null) gateway.get().signOut()
    }

    override fun start() {
        if (!config.isComplete) return
        scope.launch {
            if (sessionRepository.observeAccount().first() != null && uidProvider.uid() == null) {
                onSessionExpired()
            }
        }
    }
}
