package com.tailormyresume.core.testing.gateway

import com.tailormyresume.core.domain.GoogleAccountGateway
import com.tailormyresume.core.domain.GoogleAccountState
import com.tailormyresume.core.domain.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow

class TestGoogleAccountGateway : GoogleAccountGateway {
    override val state = MutableStateFlow<GoogleAccountState>(GoogleAccountState.Idle)
    var opens = 0
    var claims = 0
    var retries = 0
    var result: SignInResult = SignInResult.Cancelled
    override suspend fun signIn(): SignInResult = result
    override suspend fun open() {
        opens++
    }
    override suspend fun claim() {
        claims++
    }
    override suspend fun continueConflict(useProfile: Boolean, applications: Set<String>) = Unit
    override suspend fun chooseProfile(useGuest: Boolean) = Unit
    override suspend fun retry() {
        retries++
    }
    override suspend fun cancel() {
        state.value = GoogleAccountState.Idle
    }
    override suspend fun dismiss() {
        state.value = GoogleAccountState.Idle
    }
}
