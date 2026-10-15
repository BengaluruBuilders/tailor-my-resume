package com.tailormyresume.app.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseNetworkException
import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FailureMappingTest {
    @Test
    fun credentialManagerFailuresMapOneToOne() {
        assertThat(GetCredentialCancellationException().toAuthFailure()).isEqualTo(AuthFailure.Cancelled)
        assertThat(NoCredentialException().toAuthFailure()).isEqualTo(AuthFailure.NoAccount)
        assertThat(GetCredentialUnknownException().toAuthFailure()).isEqualTo(AuthFailure.Failed)
    }

    @Test
    fun firebaseFailuresMapOneToOne() {
        assertThat(FirebaseNetworkException("x").toAuthFailure()).isEqualTo(AuthFailure.Offline)
        assertThat(IllegalStateException("x").toAuthFailure()).isEqualTo(AuthFailure.Failed)
    }

    @Test
    fun everyFailureHasASignInResult() {
        val results = AuthFailure.entries.associateWith { it.toSignInResult() }

        assertThat(results.getValue(AuthFailure.Cancelled)).isEqualTo(SignInResult.Cancelled)
        assertThat(results.getValue(AuthFailure.Offline))
            .isEqualTo(SignInResult.Failed(SignInFailureReason.NetworkUnavailable))
        listOf(AuthFailure.NoAccount, AuthFailure.Failed, AuthFailure.NotConfigured).forEach {
            assertThat(results.getValue(it)).isEqualTo(SignInResult.Failed(SignInFailureReason.ProviderUnavailable))
        }
    }
}
