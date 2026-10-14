package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoteAccountWipeFinisherGuardTest {

    private val calls = mutableListOf<String>()
    private var signedIn: SignInAccount? = SignInAccount(id = "uid-b", displayName = "B", email = "b@example.com")

    private val gateway = object : SignInGateway {
        override suspend fun currentAccount(): SignInAccount? = signedIn

        override suspend fun signIn(): SignInResult = error("not used")

        override suspend fun signOut() {
            calls += "signOut"
        }
    }

    private val wiper = LocalDataWiper { calls += "wipeAll" }

    @Test
    fun anotherOpenAccountIsNeitherSignedOutNorWiped() = runTest {
        val marker = TestPendingAccountWipe(PendingWipeState.SERVER_CLOSED, markerUid = "uid-a")

        RemoteAccountWipeFinisher(gateway, wiper, marker).finish()

        assertThat(calls).isEmpty()
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun theMarkedAccountIsSignedOutAndWiped() = runTest {
        val marker = TestPendingAccountWipe(PendingWipeState.SERVER_CLOSED, markerUid = "uid-b")

        RemoteAccountWipeFinisher(gateway, wiper, marker).finish()

        assertThat(calls).containsExactly("signOut", "wipeAll").inOrder()
    }

    @Test
    fun noCurrentAccountStillWipesTheLeftovers() = runTest {
        signedIn = null
        val marker = TestPendingAccountWipe(PendingWipeState.SERVER_CLOSED, markerUid = "uid-a")

        RemoteAccountWipeFinisher(gateway, wiper, marker).finish()

        assertThat(calls).containsExactly("signOut", "wipeAll").inOrder()
    }

    @Test
    fun aMarkerWithoutAnAccountKeepsTheEarlierBehaviour() = runTest {
        val marker = TestPendingAccountWipe(PendingWipeState.SERVER_CLOSED)

        RemoteAccountWipeFinisher(gateway, wiper, marker).finish()

        assertThat(calls).containsExactly("signOut", "wipeAll").inOrder()
    }
}
