package com.tailormyresume.feature.settings.impl.referrals

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GoogleAccountGateway
import com.tailormyresume.core.domain.GoogleAccountState
import com.tailormyresume.core.domain.ReferralGateway
import com.tailormyresume.core.domain.ReferralUiState
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ReferralsViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun actionsKeepSignInIndependentAndPassExplicitReplaceChoice() = runTest {
        val calls = mutableListOf<String>()
        val gateway = object : ReferralGateway {
            override val state = MutableStateFlow<ReferralUiState>(ReferralUiState.Content())
            override suspend fun refresh() {
                calls += "refresh"
            }
            override suspend fun save(code: String, replace: Boolean) {
                calls += "$code:$replace"
            }
            override suspend fun clear() {
                calls += "clear"
            }
            override suspend fun issue() {
                calls += "issue"
            }
            override suspend fun apply() {
                calls += "apply"
            }
            override suspend fun acceptTransfer() {
                calls += "review"
            }
        }
        val google = object : GoogleAccountGateway {
            override val state = MutableStateFlow<GoogleAccountState>(GoogleAccountState.Idle)
            override suspend fun signIn(): SignInResult = SignInResult.Cancelled
            override suspend fun open() {
                calls += "google"
            }
            override suspend fun claim() = Unit
            override suspend fun continueConflict(useProfile: Boolean, applications: Set<String>) = Unit
            override suspend fun chooseProfile(useGuest: Boolean) = Unit
            override suspend fun retry() = Unit
            override suspend fun cancel() = Unit
            override suspend fun dismiss() = Unit
        }
        val viewModel = ReferralsViewModel(gateway, google)
        viewModel.save("candidate", true)
        runCurrent()
        viewModel.google()
        runCurrent()
        viewModel.review()
        runCurrent()
        assertThat(calls).containsExactly("candidate:true", "google", "review").inOrder()
    }
}
