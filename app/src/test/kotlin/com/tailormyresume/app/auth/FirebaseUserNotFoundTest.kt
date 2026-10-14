package com.tailormyresume.app.auth

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GetTokenResult
import com.tailormyresume.core.network.SessionExpiredException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class FirebaseUserNotFoundTest {

    @Test
    fun userNotFoundMarksTheAccountAsGone() {
        val gone = Tasks.forException<GetTokenResult>(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "x"))

        val thrown = assertThrows(SessionExpiredException::class.java) { offMainThread { awaitIdToken(gone) } }

        assertThat(thrown.accountGone).isTrue()
    }

    @Test
    fun userDisabledIsExpiryButNotGone() {
        val disabled = Tasks.forException<GetTokenResult>(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "x"))

        val thrown = assertThrows(SessionExpiredException::class.java) { offMainThread { awaitIdToken(disabled) } }

        assertThat(thrown.accountGone).isFalse()
    }

    @Test
    fun theTokenNeverAppearsInTheThrownMessage() {
        val gone = Tasks.forException<GetTokenResult>(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", SECRET_TOKEN))

        val thrown = assertThrows(SessionExpiredException::class.java) { offMainThread { awaitIdToken(gone) } }

        assertThat(thrown.message).doesNotContain(SECRET_TOKEN)
    }

    private fun <T> offMainThread(block: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit(Callable(block)).get()
        } catch (failure: ExecutionException) {
            throw failure.cause ?: failure
        } finally {
            executor.shutdown()
        }
    }
}
