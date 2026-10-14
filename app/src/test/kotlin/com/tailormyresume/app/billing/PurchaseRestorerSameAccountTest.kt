package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test

class PurchaseRestorerSameAccountTest {
    private val session = TestSessionRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val restores = Channel<Unit>(Channel.UNLIMITED)
    private val gateway = object : PaymentGateway by TestPaymentGateway() {
        override suspend fun restorePurchases(): PurchaseEntitlement {
            restores.send(Unit)
            return PurchaseEntitlement(0, 0, emptyList())
        }
    }

    @After
    fun tearDown() = scope.cancel()

    private fun startRestorer() = PurchaseRestorer({ gateway }, { FakePlayBilling() }, session, scope).start()

    private suspend fun awaitRestore() = withTimeout(5_000) { restores.receive() }

    @Test
    fun signingBackInAsTheSameAccountRestoresAgain() = runBlocking {
        startRestorer()

        session.saveAccount(account)
        awaitRestore()
        session.signOut()
        delay(SETTLE_MILLIS)
        session.saveAccount(account)
        awaitRestore()
    }

    @Test
    fun savingTheSameAccountAgainWithoutSigningOutRestoresOnce() = runBlocking {
        startRestorer()

        session.saveAccount(account)
        awaitRestore()
        session.saveAccount(account)
        delay(SETTLE_MILLIS)

        assertThat(restores.tryReceive().isSuccess).isFalse()
    }

    private companion object {
        const val SETTLE_MILLIS = 300L
        val account = SignInAccount("uid-1", "Priya", "p@example.com")
    }
}
