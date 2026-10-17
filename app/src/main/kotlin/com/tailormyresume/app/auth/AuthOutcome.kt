package com.tailormyresume.app.auth

import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.model.AccountMode

enum class AuthFailure {
    Cancelled,
    NoAccount,
    Offline,
    Failed,
    NotConfigured,
    CredentialConflict,
    ;

    fun toSignInResult(): SignInResult = when (this) {
        Cancelled -> SignInResult.Cancelled
        Offline -> SignInResult.Failed(SignInFailureReason.NetworkUnavailable)
        NoAccount, Failed, NotConfigured, CredentialConflict -> SignInResult.Failed(SignInFailureReason.ProviderUnavailable)
    }
}

sealed interface AuthOutcome<out T> {
    data class Success<T>(val value: T) : AuthOutcome<T>

    data class Failure(val failure: AuthFailure) : AuthOutcome<Nothing>
}

data class FirebaseUser(val uid: String, val displayName: String, val email: String, val mode: AccountMode = AccountMode.GOOGLE, val googleSubject: String? = null)

interface GoogleCredentialSource {
    suspend fun idToken(): AuthOutcome<String>

    suspend fun clearState()
}

interface FirebaseSessionClient {
    suspend fun currentUser(): FirebaseUser? = null

    suspend fun linkGoogle(googleIdToken: String): AuthOutcome<FirebaseUser> = AuthOutcome.Failure(AuthFailure.NotConfigured)

    suspend fun refreshGoogle(): AuthOutcome<FirebaseUser> = AuthOutcome.Failure(AuthFailure.NotConfigured)

    fun isAnonymous(): Boolean = false

    suspend fun hasAnonymousSession(): Boolean = isAnonymous()

    suspend fun resumeGuest(): AuthOutcome<FirebaseUser> = AuthOutcome.Failure(AuthFailure.NotConfigured)

    suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser>

    fun signOut()
}
