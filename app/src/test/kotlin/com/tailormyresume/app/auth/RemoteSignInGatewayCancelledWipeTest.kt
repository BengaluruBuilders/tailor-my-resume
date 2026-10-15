package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class RemoteSignInGatewayCancelledWipeTest {
    private val server = MockWebServer().apply { start() }
    private val session = TestSessionRepository()
    private val keptJob = KeptJobDescription(text = "pasted jd", company = "Northwind", role = "Analyst")

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    @Test
    fun cancellingAfterTheWipeStillRestoresTheKeptJdAndSavesTheAccount() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        session.saveLastAccountId("uid-0")
        session.keepJobDescription(keptJob)
        lateinit var signIn: Job
        val wiper = LocalDataWiper {
            session.clearKeptJobDescription()
            signIn.cancel()
            yield()
        }
        val gateway = RemoteSignInGateway(
            completeConfig,
            ScriptedCredentials(),
            ScriptedFirebase(),
            server.api(),
            session,
            server.signOutCleaner(),
            wiper,
        )

        signIn = launch { gateway.signIn() }
        signIn.join()

        assertThat(session.observeKeptJobDescription().first()).isEqualTo(keptJob)
        assertThat(session.observeAccount().first()?.id).isEqualTo("uid-1")
        assertThat(session.lastAccountId()).isEqualTo("uid-1")
    }
}
