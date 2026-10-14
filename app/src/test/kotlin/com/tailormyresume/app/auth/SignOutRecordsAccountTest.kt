package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class SignOutRecordsAccountTest {
    private val server = MockWebServer().apply { start() }
    private val firebase = ScriptedFirebase()
    private val session = TestSessionRepository()
    private var wipes = 0
    private val wiper = LocalDataWiper {
        wipes++
        session.clear()
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun gateway() =
        RemoteSignInGateway(completeConfig, ScriptedCredentials(), firebase, server.api(), session, server.signOutCleaner(), wiper)

    private suspend fun signedInAtUpgrade(uid: String) =
        session.saveAccount(SignInAccount(id = uid, displayName = "Old", email = "old@example.com"))

    @Test
    fun anInstallSignedInBeforeUpgradeWipesWhenAnotherAccountSignsIn() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        signedInAtUpgrade("uid-0")
        val gateway = gateway()

        gateway.signOut()
        gateway.signIn()

        assertThat(wipes).isEqualTo(1)
        assertThat(session.lastAccountId()).isEqualTo("uid-1")
    }

    @Test
    fun theSameAccountSigningBackInAfterUpgradeKeepsItsData() = runTest {
        firebase.outcome = AuthOutcome.Success(FirebaseUser("uid-0", "Priya", USER_EMAIL))
        server.enqueue(jsonResponse(200, ME_BODY))
        signedInAtUpgrade("uid-0")
        val gateway = gateway()

        gateway.signOut()
        gateway.signIn()

        assertThat(wipes).isEqualTo(0)
    }

    @Test
    fun theNewUsersOnboardingInputSurvivesTheWipe() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        val job = KeptJobDescription(text = "Analyst role", company = "Northwind", role = "Analyst")
        session.saveLastAccountId("uid-0")
        session.keepJobDescription(job)

        gateway().signIn()

        assertThat(wipes).isEqualTo(1)
        assertThat(session.observeKeptJobDescription().first()).isEqualTo(job)
        assertThat(session.lastAccountId()).isEqualTo("uid-1")
    }
}
