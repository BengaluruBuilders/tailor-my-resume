package com.tailormyresume.app.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.SessionExpiredException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebaseAuthClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: FirebaseConfig,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) : FirebaseSessionClient, IdTokenProvider, FirebaseUidProvider {

    private val auth: FirebaseAuth? by lazy { if (config.isComplete) createAuth() else null }

    override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> = withContext(io) {
        val firebaseAuth = auth ?: return@withContext AuthOutcome.Failure(AuthFailure.NotConfigured)
        try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val user = firebaseAuth.signInWithCredential(credential).await().user
                ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
            AuthOutcome.Success(FirebaseUser(user.uid, user.displayName.orEmpty(), user.email.orEmpty()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AuthOutcome.Failure(failure.toAuthFailure())
        }
    }

    override fun signOut() {
        auth?.signOut()
    }

    override fun idToken(forceRefresh: Boolean): String? {
        val firebaseAuth = auth ?: return null
        val user = firebaseAuth.currentUser ?: throw SessionExpiredException()
        return awaitIdToken(user.getIdToken(forceRefresh))
    }

    override fun uid(): String? = auth?.currentUser?.uid

    private fun createAuth(): FirebaseAuth {
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey(config.apiKey)
                .setApplicationId(config.appId)
                .setProjectId(config.projectId)
                .build(),
        )
        return FirebaseAuth.getInstance(app)
    }
}

internal fun Exception.toAuthFailure(): AuthFailure = when (this) {
    is FirebaseNetworkException -> AuthFailure.Offline
    else -> AuthFailure.Failed
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}

private const val TOKEN_TIMEOUT_SECONDS = 10L

internal fun awaitIdToken(task: Task<GetTokenResult>, timeoutSeconds: Long = TOKEN_TIMEOUT_SECONDS): String? = try {
    Tasks.await(task, timeoutSeconds, TimeUnit.SECONDS).token
} catch (failure: Exception) {
    when {
        failure.isSessionExpiry() -> throw SessionExpiredException(accountGone = failure.isUserNotFound())
        failure is TimeoutException -> throw SocketTimeoutException("Token fetch timed out")
        failure is InterruptedException -> {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Token fetch interrupted")
        }
        else -> throw IOException("Token fetch failed: ${failure.javaClass.simpleName}")
    }
}

internal fun Throwable.isUserNotFound(): Boolean =
    (if (this is ExecutionException) cause else this).let { it is FirebaseAuthInvalidUserException && it.errorCode == USER_NOT_FOUND }

private const val USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"

internal fun Throwable.isSessionExpiry(): Boolean = when (if (this is ExecutionException) cause else this) {
    is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> true
    else -> false
}
