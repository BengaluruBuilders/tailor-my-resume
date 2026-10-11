package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInOutcome
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.domain.onboarding.ObserveStartDestinationUseCase
import com.tailormyresume.core.domain.onboarding.StartDestination
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class SignInViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val session = TestSessionRepository()
    private val profile = TestProfileRepository()
    private val startDestination = ObserveStartDestinationUseCase(session, profile)

    @Test
    fun signInCalledOnceOnDoubleTap() {
        val gate = CompletableDeferred<SignInResult>()
        val gateway = ScriptedSignInGateway { gate.await() }
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()
        viewModel.onContinueWithGoogle()

        assertThat(gateway.calls).isEqualTo(1)
    }

    @Test
    fun signingInStateWhileGatewaySuspended() {
        val gate = CompletableDeferred<SignInResult>()
        val viewModel = SignInViewModel(ScriptedSignInGateway { gate.await() })
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Ready)

        viewModel.onContinueWithGoogle()
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.SigningIn)

        gate.complete(SignInResult.Cancelled)
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Cancelled)
    }

    @Test
    fun signedInLeavesRoutingToSessionGate() = runTest {
        val gateway = TestSignInGateway(session)
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()

        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.SigningIn)
        assertThat(startDestination().first()).isEqualTo(StartDestination.Upload)
        session.markOnboardingComplete()
        assertThat(startDestination().first()).isEqualTo(StartDestination.Applications)
    }

    @Test
    fun cancelKeepsNoData() = runTest {
        val gateway = TestSignInGateway(session).withOutcome(SignInOutcome.Cancelled)
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()

        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Cancelled)
        assertThat(session.observeAccount().first()).isNull()
        assertThat(session.observeOnboardingComplete().first()).isFalse()
        assertThat(session.observeKeptJobDescription().first()).isNull()
        assertThat(profile.observeProfile().first()).isNull()
        assertThat(startDestination().first()).isEqualTo(StartDestination.SignIn)
    }

    @Test
    fun retryCallsSignInAgain() {
        val gateway = ScriptedSignInGateway { SignInResult.Cancelled }
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()
        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Cancelled)
        viewModel.onContinueWithGoogle()

        assertThat(gateway.calls).isEqualTo(2)
    }

    @Test
    fun failedShowsSameCancelledState() {
        val gateway = ScriptedSignInGateway { SignInResult.Failed(SignInFailureReason.NetworkUnavailable) }
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()

        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Cancelled)
    }

    @Test
    fun gatewayThrowsMapsToSameState() {
        val gateway = ScriptedSignInGateway { error("provider exploded") }
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()

        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.Cancelled)
    }

    @Test
    fun coroutineCancellationIsNotSwallowed() {
        val gateway = ScriptedSignInGateway { throw CancellationException("scope ended") }
        val viewModel = SignInViewModel(gateway)

        viewModel.onContinueWithGoogle()

        assertThat(viewModel.uiState.value).isEqualTo(SignInUiState.SigningIn)
    }
}
