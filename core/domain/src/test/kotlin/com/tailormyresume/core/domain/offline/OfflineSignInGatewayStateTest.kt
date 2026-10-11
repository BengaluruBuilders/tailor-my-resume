package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockOperation
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInOutcome
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class OfflineSignInGatewayStateTest {

    private val session = TestSessionRepository()

    @Test
    fun aSuccessfulSignInIsWrittenToTheSession() = runTest {
        OfflineSignInGateway(session, NoMockLatency, TestMockStateStore()).signIn()

        assertThat(session.observeAccount().first()).isEqualTo(SignInAccount.localAccount)
    }

    @Test
    fun aSecondGatewayOverTheSameSessionSeesTheAccount() = runTest {
        OfflineSignInGateway(session, NoMockLatency, TestMockStateStore()).signIn()

        assertThat(OfflineSignInGateway(session, NoMockLatency, TestMockStateStore()).currentAccount()).isEqualTo(SignInAccount.localAccount)
    }

    @Test
    fun aFailedSignInWritesNothing() = runTest {
        OfflineSignInGateway(session, NoMockLatency, TestMockStateStore()).withOutcome(SignInOutcome.Failed).signIn()

        assertThat(session.observeAccount().first()).isNull()
    }

    @Test
    fun signOutKeepsTheOnboardingFlag() = runTest {
        val gateway = OfflineSignInGateway(session, NoMockLatency, TestMockStateStore())
        gateway.signIn()
        session.markOnboardingComplete()

        gateway.signOut()

        assertThat(session.observeAccount().first()).isNull()
        assertThat(session.observeOnboardingComplete().first()).isTrue()
    }

    @Test
    fun signInWaitsForTheLatencyPolicy() = runTest {
        val awaited = mutableListOf<MockOperation>()
        val latency = object : MockLatency {
            override suspend fun await(operation: MockOperation) {
                awaited += operation
            }
        }

        OfflineSignInGateway(session, latency, TestMockStateStore()).signIn()

        assertThat(awaited).containsExactly(MockOperation.SIGN_IN)
    }

    @Test
    fun aNewSignInAfterSignOutKeepsTheSpentCredit() = runTest {
        val store = TestMockStateStore()
        val payment = OfflinePaymentGateway(store, NoMockLatency, TestClock(Instant.fromEpochSeconds(1)), TestIdGenerator("order"))
        val gateway = OfflineSignInGateway(session, NoMockLatency, store)
        gateway.signIn()
        payment.unlock("application-1")
        assertThat(payment.entitlement().totalCredits).isEqualTo(0)

        gateway.signOut()
        gateway.signIn()

        assertThat(payment.entitlement().totalCredits).isEqualTo(0)
    }

    @Test
    fun aNewSignInAfterSignOutKeepsThePurchasedCredits() = runTest {
        val store = TestMockStateStore()
        val payment = OfflinePaymentGateway(store, NoMockLatency, TestClock(Instant.fromEpochSeconds(1)), TestIdGenerator("order"))
        val gateway = OfflineSignInGateway(session, NoMockLatency, store)
        gateway.signIn()
        payment.purchase(ApplicationPack.APPLICATION_PACK_FIVE)
        val before = payment.entitlement()

        gateway.signOut()
        gateway.signIn()

        assertThat(payment.entitlement()).isEqualTo(before)
    }

    @Test
    fun aNewSignInAfterTheCreditsWereClearedStartsWithTheFreeCredit() = runTest {
        val store = TestMockStateStore()
        val payment = OfflinePaymentGateway(store, NoMockLatency, TestClock(Instant.fromEpochSeconds(1)), TestIdGenerator("order"))
        val gateway = OfflineSignInGateway(session, NoMockLatency, store)
        gateway.signIn()
        payment.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        payment.clearCredits()
        assertThat(payment.entitlement().totalCredits).isEqualTo(0)
        gateway.signOut()
        gateway.signIn()

        assertThat(payment.entitlement().freeCredits).isEqualTo(1)
        assertThat(payment.entitlement().purchasedCredits).isEqualTo(0)
    }
}
