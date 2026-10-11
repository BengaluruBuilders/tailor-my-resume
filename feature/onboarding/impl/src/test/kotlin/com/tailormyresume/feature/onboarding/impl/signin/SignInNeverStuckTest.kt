package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Rule
import org.junit.Test

class SignInNeverStuckTest {

    private val scheduler = StandardTestDispatcher()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(scheduler)

    private val testScope = TestScope(scheduler)

    @Test
    fun aGatewayThatNeverReturnsEndsInTheCancelledState() = testScope.run {
        val viewModel = SignInViewModel(ScriptedSignInGateway { awaitCancellation() })

        viewModel.onContinueWithGoogle()
        runCurrent()
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.SigningIn)

        advanceTimeBy(31_000)
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Cancelled)
    }

    @Test
    fun aSignInThatSucceededButWasNotHandedOverStopsSpinning() = testScope.run {
        val account = SignInAccount("a", "A", "a@example.com")
        val viewModel = SignInViewModel(ScriptedSignInGateway { SignInResult.SignedIn(account) })

        viewModel.onContinueWithGoogle()
        runCurrent()
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.SigningIn)

        advanceTimeBy(6_000)
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Ready)
    }
}
