package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class RemoteSignInGatewayTest {
    private val server = MockWebServer().apply { start() }
    private val credentials = ScriptedCredentials()
    private val firebase = ScriptedFirebase()
    private val session = TestSessionRepository()
    private val cleaner = server.signOutCleaner()

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun gateway(config: FirebaseConfig = completeConfig) =
        RemoteSignInGateway(config, credentials, firebase, server.api(), session, cleaner)

    private fun failed(reason: SignInFailureReason) = SignInResult.Failed(reason)

    @Test
    fun signInCallsMeAndKeepsTheAccount() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))

        val result = gateway().signIn()

        assertThat(result).isInstanceOf(SignInResult.SignedIn::class.java)
        assertThat(server.takeRequest().path).isEqualTo("/v1/me")
        assertThat(session.observeAccount().first()?.id).isEqualTo("uid-1")
        assertThat(firebase.signedOut).isFalse()
    }

    @Test
    fun aBlankDisplayNameFallsBackToTheEmail() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        firebase.outcome = AuthOutcome.Success(FirebaseUser("uid-1", "", USER_EMAIL))

        gateway().signIn()

        assertThat(session.observeAccount().first()?.displayName).isEqualTo(USER_EMAIL)
    }

    @Test
    fun anIncompleteConfigFailsBeforeAnyCall() = runTest {
        val result = gateway(completeConfig.copy(webClientId = "")).signIn()

        assertThat(result).isEqualTo(failed(SignInFailureReason.ProviderUnavailable))
        assertThat(credentials.requests).isEqualTo(0)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun everyEmptyConfigFieldCountsAsNotConfigured() {
        listOf(
            completeConfig.copy(apiKey = ""),
            completeConfig.copy(appId = " "),
            completeConfig.copy(projectId = ""),
            completeConfig.copy(webClientId = ""),
        ).forEach { assertThat(it.isComplete).isFalse() }
        assertThat(completeConfig.isComplete).isTrue()
    }

    @Test
    fun aCancelledChooserIsCancelled() = runTest {
        credentials.outcome = AuthOutcome.Failure(AuthFailure.Cancelled)

        assertThat(gateway().signIn()).isEqualTo(SignInResult.Cancelled)
        assertThat(session.observeAccount().first()).isNull()
    }

    @Test
    fun noGoogleAccountIsProviderUnavailable() = runTest {
        credentials.outcome = AuthOutcome.Failure(AuthFailure.NoAccount)

        assertThat(gateway().signIn()).isEqualTo(failed(SignInFailureReason.ProviderUnavailable))
    }

    @Test
    fun aFirebaseNetworkFailureIsNetworkUnavailable() = runTest {
        firebase.outcome = AuthOutcome.Failure(AuthFailure.Offline)

        assertThat(gateway().signIn()).isEqualTo(failed(SignInFailureReason.NetworkUnavailable))
    }

    @Test
    fun aFirebaseFailureIsProviderUnavailable() = runTest {
        firebase.outcome = AuthOutcome.Failure(AuthFailure.Failed)

        assertThat(gateway().signIn()).isEqualTo(failed(SignInFailureReason.ProviderUnavailable))
    }

    @Test
    fun aServerRefusalOfMeSignsFirebaseOutAndFails() = runTest {
        server.enqueue(errorResponse(401, "INVALID_TOKEN"))
        server.enqueue(errorResponse(401, "INVALID_TOKEN"))

        val result = gateway().signIn()

        assertThat(result).isEqualTo(failed(SignInFailureReason.ProviderUnavailable))
        assertThat(firebase.signedOut).isTrue()
        assertThat(session.observeAccount().first()).isNull()
    }

    @Test
    fun anUnreachableServerIsNetworkUnavailable() = runTest {
        val gateway = gateway()
        server.shutdown()

        assertThat(gateway.signIn()).isEqualTo(failed(SignInFailureReason.NetworkUnavailable))
        assertThat(firebase.signedOut).isTrue()
    }

    @Test
    fun signOutClearsFirebaseCredentialsAndTheSession() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))
        val gateway = gateway()
        gateway.signIn()

        gateway.signOut()

        assertThat(firebase.signedOut).isTrue()
        assertThat(credentials.cleared).isTrue()
        assertThat(gateway.currentAccount()).isNull()
    }

    @Test
    fun noTokenOrEmailReachesTheConsole() = runTest {
        val captured = ByteArrayOutputStream()
        val originalOut = System.out
        val originalErr = System.err
        System.setOut(PrintStream(captured))
        System.setErr(PrintStream(captured))
        try {
            server.enqueue(jsonResponse(200, ME_BODY))
            gateway().signIn()
            firebase.outcome = AuthOutcome.Failure(AuthFailure.Failed)
            gateway().signIn()
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }

        assertThat(captured.toString()).doesNotContain(SECRET_TOKEN)
        assertThat(captured.toString()).doesNotContain(USER_EMAIL)
    }
}
