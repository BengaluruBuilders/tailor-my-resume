package com.tailormyresume.app.auth

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GetTokenResult
import com.tailormyresume.core.network.SessionExpiredException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
class FirebaseAuthClientExpiryTest {
    @Test
    fun invalidUserAndInvalidCredentialsAreSessionExpiry() {
        val disabled = ExecutionException(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "disabled"))
        val revoked = ExecutionException(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "revoked"))

        assertThat(disabled.isSessionExpiry()).isTrue()
        assertThat(revoked.isSessionExpiry()).isTrue()
    }

    @Test
    fun timeoutAndNetworkFailuresAreNotSessionExpiry() {
        assertThat(TimeoutException().isSessionExpiry()).isFalse()
        assertThat(ExecutionException(FirebaseNetworkException("offline")).isSessionExpiry()).isFalse()
        assertThat(ExecutionException(RuntimeException("other")).isSessionExpiry()).isFalse()
        assertThat(InterruptedException().isSessionExpiry()).isFalse()
    }

    @Test
    fun idTokenReturnsTheTokenOnSuccess() {
        assertThat(offMainThread { awaitIdToken(Tasks.forResult(GetTokenResult("tok", emptyMap()))) }).isEqualTo("tok")
    }

    @Test
    fun idTokenTimeoutIsASocketTimeout() {
        val never = TaskCompletionSource<GetTokenResult>().task
        assertThrows(SocketTimeoutException::class.java) { offMainThread { awaitIdToken(never, timeoutSeconds = 0) } }
    }

    @Test
    fun idTokenTransientFailureIsAnIoException() {
        val failed = Tasks.forException<GetTokenResult>(FirebaseNetworkException("offline"))
        val thrown = assertThrows(IOException::class.java) { offMainThread { awaitIdToken(failed) } }
        assertThat(thrown).isNotInstanceOf(SocketTimeoutException::class.java)
    }

    @Test
    fun idTokenExpiryRethrowsAsSessionExpired() {
        val revoked = Tasks.forException<GetTokenResult>(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "x"))
        assertThrows(SessionExpiredException::class.java) { offMainThread { awaitIdToken(revoked) } }
    }

    private fun <T> offMainThread(block: () -> T): T {
        val outcome = Executors.newSingleThreadExecutor().let { executor ->
            try {
                executor.submit(Callable(block)).get()
            } catch (failure: ExecutionException) {
                throw failure.cause ?: failure
            } finally {
                executor.shutdown()
            }
        }
        return outcome
    }
}
