package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class AccountSwitchSignInTest {
    private val server = MockWebServer().apply { start() }
    private val credentials = ScriptedCredentials()
    private val firebase = ScriptedFirebase()
    private val session = TestSessionRepository()
    private val wipes = mutableListOf<SignInAccount?>()
    private val wiper = LocalDataWiper { wipes += session.observeAccount().first() }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun gateway() =
        RemoteSignInGateway(completeConfig, credentials, firebase, server.api(), session, server.signOutCleaner(), wiper)

    @Test
    fun differentUidSignInWipesPreviousAccountDataBeforeReturningSignedIn() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        session.saveLastAccountId("uid-0")

        val result = gateway().signIn()

        assertThat(result).isInstanceOf(SignInResult.SignedIn::class.java)
        assertThat(wipes).containsExactly(null)
        assertThat(session.observeAccount().first()?.id).isEqualTo("uid-1")
        assertThat(session.lastAccountId()).isEqualTo("uid-1")
    }

    @Test
    fun sameUidSignInKeepsLocalData() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        session.saveLastAccountId("uid-1")

        gateway().signIn()

        assertThat(wipes).isEmpty()
        assertThat(session.lastAccountId()).isEqualTo("uid-1")
    }

    @Test
    fun unknownPreviousUidKeepsLocalDataAndRecordsUid() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))

        gateway().signIn()

        assertThat(wipes).isEmpty()
        assertThat(session.lastAccountId()).isEqualTo("uid-1")
    }

    @Test
    fun aRefusedMeCallLeavesTheLocalDataAndTheLastAccountAlone() = runTest {
        server.enqueue(errorResponse(401, "INVALID_TOKEN"))
        server.enqueue(errorResponse(401, "INVALID_TOKEN"))
        session.saveLastAccountId("uid-0")

        gateway().signIn()

        assertThat(wipes).isEmpty()
        assertThat(session.lastAccountId()).isEqualTo("uid-0")
    }

    @Test
    fun signOutKeepsTheLastAccountSoTheSameAccountReturnsToItsData() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        server.enqueue(jsonResponse(200, ME_BODY))
        val gateway = gateway()
        gateway.signIn()

        gateway.signOut()
        gateway.signIn()

        assertThat(wipes).isEmpty()
    }
}
