package com.tailormyresume.app.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.onboarding.ObserveStartDestinationUseCase
import com.tailormyresume.core.domain.onboarding.StartDestination
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AppViewModelDeleteThenSignInTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val session = TestSessionRepository()
    private val account = SignInAccount("account-1", "First", "first@example.com")

    @Test
    fun signingInAgainAfterDeleteRoutesAwayFromSignIn() = runTest {
        session.sendAccount(account)
        session.sendOnboardingComplete(true)
        val viewModel = AppViewModel(session, ObserveStartDestinationUseCase(session, TestProfileRepository()), TestCreditsRepository())
        viewModel.rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, account.id))

            session.signOut()
            session.clear()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.SignIn, null))

            session.saveAccount(account)
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, account.id))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
