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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.model.AccountMode
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

    override suspend fun currentUser(): FirebaseUser? = withContext(io) {
        auth?.currentUser?.let { user ->
            FirebaseUser(user.uid, user.displayName.orEmpty(), user.email.orEmpty(), if (user.isAnonymous) AccountMode.ANONYMOUS else AccountMode.GOOGLE, user.providerData.firstOrNull { it.providerId == "google.com" }?.uid)
        }
    }

    override suspend fun linkGoogle(googleIdToken: String): AuthOutcome<FirebaseUser> = withContext(io) {
        val user = auth?.currentUser?.takeIf { it.isAnonymous }
            ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
        try {
            val linked = user.linkWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await().user
                ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
            check(linked.uid == user.uid)
            refreshGoogle()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (conflict: FirebaseAuthUserCollisionException) {
            AuthOutcome.Failure(AuthFailure.CredentialConflict)
        } catch (failure: Exception) {
            AuthOutcome.Failure(failure.toAuthFailure())
        }
    }

    override suspend fun refreshGoogle(): AuthOutcome<FirebaseUser> = withContext(io) {
        val user = auth?.currentUser ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
        try {
            user.reload().await()
            check(!user.isAnonymous && user.providerData.any { it.providerId == "google.com" })
            user.getIdToken(true).await()
            AuthOutcome.Success(FirebaseUser(user.uid, user.displayName.orEmpty(), user.email.orEmpty(), googleSubject = user.providerData.firstOrNull { it.providerId == "google.com" }?.uid))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AuthOutcome.Failure(failure.toAuthFailure())
        }
    }

    override suspend fun reauthenticateGoogle(googleIdToken: String, expectedUid: String, expectedGoogleDigest: String): AuthOutcome<FirebaseUser> = withContext(io) {
        val firebaseAuth = auth ?: return@withContext AuthOutcome.Failure(AuthFailure.NotConfigured)
        val user = firebaseAuth.currentUser ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
        try {
            fun verifyCurrent() {
                val current = checkNotNull(firebaseAuth.currentUser)
                check(current.uid == expectedUid && !current.isAnonymous)
                check(current.providerData.single { it.providerId == "google.com" }.uid.let(GoogleCredentialIdentity::digestSubject) == expectedGoogleDigest)
            }
            verifyCurrent()
            check(GoogleCredentialIdentity.digest(googleIdToken) == expectedGoogleDigest)
            user.reauthenticate(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
            verifyCurrent()
            user.reload().await()
            verifyCurrent()
            user.getIdToken(true).await()
            verifyCurrent()
            AuthOutcome.Success(checkNotNull(currentUser()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AuthOutcome.Failure(failure.toAuthFailure())
        }
    }

    override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> = withContext(io) {
        val firebaseAuth = auth ?: return@withContext AuthOutcome.Failure(AuthFailure.NotConfigured)
        try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val user = firebaseAuth.signInWithCredential(credential).await().user
                ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
            AuthOutcome.Success(FirebaseUser(user.uid, user.displayName.orEmpty(), user.email.orEmpty(), googleSubject = user.providerData.firstOrNull { it.providerId == "google.com" }?.uid))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AuthOutcome.Failure(failure.toAuthFailure())
        }
    }

    override fun isAnonymous(): Boolean = auth?.currentUser?.isAnonymous == true

    override suspend fun hasAnonymousSession(): Boolean = withContext(io) { isAnonymous() }

    override suspend fun resumeGuest(): AuthOutcome<FirebaseUser> = withContext(io) {
        val firebaseAuth = auth ?: return@withContext AuthOutcome.Failure(AuthFailure.NotConfigured)
        try {
            val current = firebaseAuth.currentUser
            if (current != null && !current.isAnonymous) return@withContext AuthOutcome.Failure(AuthFailure.Failed)
            val user = current ?: firebaseAuth.signInAnonymously().await().user
                ?: return@withContext AuthOutcome.Failure(AuthFailure.Failed)
            AuthOutcome.Success(FirebaseUser(user.uid, "", "", AccountMode.ANONYMOUS))
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
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
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
