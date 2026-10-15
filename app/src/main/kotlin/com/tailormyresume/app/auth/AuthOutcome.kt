package com.tailormyresume.app.auth

import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInResult

enum class AuthFailure {
    Cancelled,
    NoAccount,
    Offline,
    Failed,
    NotConfigured,
    ;

    fun toSignInResult(): SignInResult = when (this) {
        Cancelled -> SignInResult.Cancelled
        Offline -> SignInResult.Failed(SignInFailureReason.NetworkUnavailable)
        NoAccount, Failed, NotConfigured -> SignInResult.Failed(SignInFailureReason.ProviderUnavailable)
    }
}

sealed interface AuthOutcome<out T> {
    data class Success<T>(val value: T) : AuthOutcome<T>

    data class Failure(val failure: AuthFailure) : AuthOutcome<Nothing>
}

data class FirebaseUser(val uid: String, val displayName: String, val email: String)

interface GoogleCredentialSource {
    suspend fun idToken(): AuthOutcome<String>

    suspend fun clearState()
}

interface FirebaseSessionClient {
    suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser>

    fun signOut()
}
