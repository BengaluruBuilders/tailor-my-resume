package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.FinishPendingAccountWipeUseCase
import com.tailormyresume.core.domain.account.PendingWipeOutcome
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestSessionRepository
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SessionExpiryHandlerAccountGoneTest {
    private val session = TestSessionRepository()
    private val gateway = SignOutGateway(session)
    private val marker = TestPendingAccountWipe()

    private fun TestScope.handler() = SessionExpiryHandler(
        session,
        Lazy { gateway },
        FirebaseUidProvider { "uid-1" },
        completeConfig,
        TestScope(testScheduler),
        marker,
    )

    private suspend fun signedInWithRequestedMarker(markerUid: String? = "uid-1") {
        session.saveAccount(SignInAccount("uid-1", "Priya", USER_EMAIL))
        session.saveLastAccountId("uid-1")
        marker.current = PendingWipeState.REQUESTED
        marker.markerUid = markerUid
    }

    @Test
    fun goneSignalConsumedByNonProbeCallPromotesRequestedMarker() = runTest(UnconfinedTestDispatcher()) {
        signedInWithRequestedMarker()

        handler().onSessionExpired(true)
        testScheduler.advanceUntilIdle()

        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)
        assertThat(gateway.signOuts).isEqualTo(1)
        assertThat(session.observeAccount().first()).isNull()
    }

    @Test
    fun laterFinishStillWipes() = runTest(UnconfinedTestDispatcher()) {
        signedInWithRequestedMarker()
        handler().onSessionExpired(true)
        testScheduler.advanceUntilIdle()
        var wipes = 0
        val deleter = ServerAccountDeleter { Result.success(Unit) }

        val outcome = FinishPendingAccountWipeUseCase(
            marker,
            object : AccountWipeFinisher {
                override suspend fun finish() {
                    wipes++
                }
            },
            deleter,
        )()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.FINISHED)
        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun markerOfAnotherUidIsNotPromoted() = runTest(UnconfinedTestDispatcher()) {
        signedInWithRequestedMarker(markerUid = "uid-2")

        handler().onSessionExpired(true)
        testScheduler.advanceUntilIdle()

        assertThat(marker.current).isEqualTo(PendingWipeState.REQUESTED)
        assertThat(gateway.signOuts).isEqualTo(1)
    }

    @Test
    fun goneSignalIsNotDroppedWhileSignOutIsInFlight() = runTest(UnconfinedTestDispatcher()) {
        signedInWithRequestedMarker()
        gateway.hold = CompletableDeferred()
        val handler = handler()

        handler.onSessionExpired(false)
        handler.onSessionExpired(true)
        gateway.hold?.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)
    }

    @Test
    fun plainExpiryNeverPromotes() = runTest(UnconfinedTestDispatcher()) {
        signedInWithRequestedMarker()

        handler().onSessionExpired(false)
        testScheduler.advanceUntilIdle()

        assertThat(marker.current).isEqualTo(PendingWipeState.REQUESTED)
        assertThat(gateway.signOuts).isEqualTo(1)
    }

    private class SignOutGateway(private val session: TestSessionRepository) : SignInGateway {
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
