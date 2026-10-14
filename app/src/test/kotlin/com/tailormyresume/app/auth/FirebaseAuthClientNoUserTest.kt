package com.tailormyresume.app.auth

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.tailormyresume.core.network.SessionExpiredException
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FirebaseAuthClientNoUserTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    private fun clientWithNoSignedInUser(): FirebaseAuthClient {
        val client = FirebaseAuthClient(context, completeConfig, Dispatchers.Unconfined)
        val signedOut = unsafe().allocateInstance(FirebaseAuth::class.java) as FirebaseAuth
        val delegate = FirebaseAuthClient::class.java.getDeclaredField("auth\$delegate")
        delegate.isAccessible = true
        delegate.set(client, lazyOf(signedOut))
        return client
    }

    private fun unsafe(): sun.misc.Unsafe {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        return field.get(null) as sun.misc.Unsafe
    }

    @Test
    fun noCurrentUserWithFirebaseConfiguredIsASessionExpiry() {
        val client = clientWithNoSignedInUser()

        assertThrows(SessionExpiredException::class.java) { client.idToken(forceRefresh = false) }
        assertThrows(SessionExpiredException::class.java) { client.idToken(forceRefresh = true) }
    }

    @Test
    fun withoutFirebaseConfigThereIsNoTokenAndNoExpiry() {
        val unconfigured = FirebaseConfig(apiKey = "", appId = "", projectId = "", webClientId = "")
        val client = FirebaseAuthClient(context, unconfigured, Dispatchers.Unconfined)

        assertThat(client.idToken(forceRefresh = false)).isNull()
    }
}
