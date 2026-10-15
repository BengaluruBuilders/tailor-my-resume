package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoteAccountWipeFinisherTest {

    private val calls = mutableListOf<String>()

    private val gateway = object : SignInGateway {
        override suspend fun currentAccount(): SignInAccount? = null

        override suspend fun signIn(): SignInResult = error("not used")

        override suspend fun signOut() {
            calls += "signOut"
        }
    }

    private val wiper = LocalDataWiper { calls += "wipeAll" }

    @Test
    fun signsOutThenWipesAndIsIdempotent() = runTest {
        val finisher = RemoteAccountWipeFinisher(gateway, wiper)

        finisher.finish()
        finisher.finish()

        assertThat(calls).containsExactly("signOut", "wipeAll", "signOut", "wipeAll").inOrder()
    }
}
