package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test

class SlowSignInTest {

    private val scheduler = StandardTestDispatcher()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(scheduler)

    private val testScope = TestScope(scheduler)

    @Test
    fun aSlowSuccessfulSignInIsNeverReportedAsCancelled() = testScope.run {
        val account = SignInAccount("a", "A", "a@example.com")
        val gateway = ScriptedSignInGateway {
            delay(35_000)
            SignInResult.SignedIn(account)
        }
        val viewModel = SignInViewModel(gateway)
        var cancelledEvents = 0
        val collector = backgroundScope.launch { viewModel.cancelled.collect { cancelledEvents += 1 } }

        viewModel.onContinueWithGoogle()
        advanceTimeBy(36_000)
        advanceUntilIdle()

        assertThat(gateway.calls).isEqualTo(1)
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.SigningIn)
        assertThat(cancelledEvents).isEqualTo(0)
    }
}
