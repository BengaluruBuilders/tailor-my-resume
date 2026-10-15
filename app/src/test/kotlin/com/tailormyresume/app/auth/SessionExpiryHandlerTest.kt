package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestSessionRepository
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SessionExpiryHandlerTest {
    private val session = TestSessionRepository()
    private val gateway = CountingGateway(session)
    private var firebaseUid: String? = "uid-1"

    private fun TestScope.handler(config: FirebaseConfig = completeConfig) = SessionExpiryHandler(
        session,
        Lazy { gateway },
        FirebaseUidProvider { firebaseUid },
        config,
        TestScope(testScheduler),
    )

    private suspend fun storeAccount() = session.saveAccount(SignInAccount("uid-1", "Priya", USER_EMAIL))

    @Test
    fun expirySignsTheStoredAccountOutAndSendsTheUserToFirstRun() = runTest(UnconfinedTestDispatcher()) {
        storeAccount()

        handler().onSessionExpired()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(1)
        assertThat(session.observeAccount().first()).isNull()
    }

    @Test
    fun expiryWithNoStoredAccountDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        handler().onSessionExpired()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(0)
    }

    @Test
    fun startWithStoredAccountAndNoFirebaseUserSignsOut() = runTest(UnconfinedTestDispatcher()) {
        storeAccount()
        firebaseUid = null

        handler().start()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(1)
    }

    @Test
    fun startWithAFirebaseUserKeepsTheAccount() = runTest(UnconfinedTestDispatcher()) {
        storeAccount()

        handler().start()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(0)
        assertThat(session.observeAccount().first()).isNotNull()
    }

    @Test
    fun startWithNoStoredAccountDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        firebaseUid = null

        handler().start()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(0)
    }

    @Test
    fun startWithIncompleteConfigDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        storeAccount()
        firebaseUid = null

        handler(completeConfig.copy(apiKey = "")).start()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(0)
    }

    @Test
    fun parallelSignalsSignOutOnce() = runTest(UnconfinedTestDispatcher()) {
        storeAccount()
        gateway.hold = CompletableDeferred()
        val handler = handler()

        repeat(5) { handler.onSessionExpired() }
        testScheduler.advanceUntilIdle()
        gateway.hold?.complete(Unit)
        testScheduler.advanceUntilIdle()
        handler.onSessionExpired()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(1)
    }

    @Test
    fun theGuardResetsSoASecondExpiryAfterANewSignInSignsOutAgain() = runTest(UnconfinedTestDispatcher()) {
        storeAccount()
        val handler = handler()

        handler.onSessionExpired()
        testScheduler.advanceUntilIdle()
        storeAccount()
        handler.onSessionExpired()
        testScheduler.advanceUntilIdle()

        assertThat(gateway.signOuts).isEqualTo(2)
    }

    private class CountingGateway(private val session: TestSessionRepository) : SignInGateway {
        var signOuts = 0
        var hold: CompletableDeferred<Unit>? = null

        override suspend fun currentAccount(): SignInAccount? = session.observeAccount().first()

        override suspend fun signIn(): SignInResult = error("not used")

        override suspend fun signOut() {
            signOuts++
            hold?.await()
            session.signOut()
        }
    }
}
