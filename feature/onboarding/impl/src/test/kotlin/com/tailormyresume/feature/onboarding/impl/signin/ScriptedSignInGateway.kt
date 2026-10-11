package com.tailormyresume.feature.onboarding.impl.signin

import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult

class ScriptedSignInGateway(
    private val script: suspend () -> SignInResult,
) : SignInGateway {
    var calls = 0
        private set

    override suspend fun currentAccount(): SignInAccount? = null

    override suspend fun signIn(): SignInResult {
        calls += 1
        return script()
    }

    override suspend fun signOut() = Unit
}
