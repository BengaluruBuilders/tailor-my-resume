package com.tailormyresume.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialManagerGoogleSource @Inject constructor(
    @ApplicationContext context: Context,
    private val config: FirebaseConfig,
    private val foreground: ForegroundActivity,
) : GoogleCredentialSource {

    private val manager = CredentialManager.create(context)

    override suspend fun idToken(): AuthOutcome<String> {
        val activity = foreground.current() ?: return AuthOutcome.Failure(AuthFailure.Failed)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(config.webClientId).build())
            .build()
        return try {
            val credential = manager.getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                AuthOutcome.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                AuthOutcome.Failure(AuthFailure.Failed)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: GetCredentialException) {
            AuthOutcome.Failure(failure.toAuthFailure())
        } catch (failure: GoogleIdTokenParsingException) {
            AuthOutcome.Failure(AuthFailure.Failed)
        }
    }

    override suspend fun clearState() {
        try {
            manager.clearCredentialState(ClearCredentialStateRequest())
        } catch (failure: ClearCredentialException) {
            return
        }
    }
}

internal fun GetCredentialException.toAuthFailure(): AuthFailure = when (this) {
    is GetCredentialCancellationException -> AuthFailure.Cancelled
    is NoCredentialException -> AuthFailure.NoAccount
    else -> AuthFailure.Failed
}
