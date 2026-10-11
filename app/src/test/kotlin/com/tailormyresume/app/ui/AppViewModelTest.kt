package com.tailormyresume.app.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.offline.OfflineSignInGateway
import com.tailormyresume.core.domain.onboarding.ObserveStartDestinationUseCase
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val sessionRepository = TestSessionRepository()

    private fun viewModel() = AppViewModel(ObserveStartDestinationUseCase(sessionRepository, TestProfileRepository()))

    @Test
    fun rootState_beforeAnyValueIsCollected_isLoading() {
        assertThat(viewModel().rootState.value).isEqualTo(AppRootState.Loading)
    }

    @Test
    fun rootState_whenOnboardingIsNotComplete_isFirstRun() = runTest {
        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.FirstRun)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_whenOnboardingIsComplete_isMainWithoutShowingFirstRun() = runTest {
        sessionRepository.sendAccount(SignInAccount.localAccount)
        sessionRepository.sendOnboardingComplete(true)

        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.Main)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_followsTheOnboardingFlagInBothDirections() = runTest {
        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.FirstRun)

            sessionRepository.saveAccount(SignInAccount.localAccount)
            sessionRepository.markOnboardingComplete()
            assertThat(awaitItem()).isEqualTo(AppRootState.Main)

            sessionRepository.signOut()
            assertThat(awaitItem()).isEqualTo(AppRootState.FirstRun)

            sessionRepository.saveAccount(SignInAccount.localAccount)
            assertThat(awaitItem()).isEqualTo(AppRootState.Main)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_afterOfflineGatewaySignOut_goesFromMainToFirstRun() = runTest {
        val gateway = OfflineSignInGateway(sessionRepository, NoMockLatency, TestMockStateStore())
        gateway.signIn()
        sessionRepository.markOnboardingComplete()

        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.Main)

            gateway.signOut()

            assertThat(awaitItem()).isEqualTo(AppRootState.FirstRun)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_signOutThenSameAccountSignIn_returnsToMainWithoutWipe() = runTest {
        val store = TestMockStateStore()
        store.write(LOCAL_DATA_KEY, LOCAL_DATA_VALUE)
        val gateway = OfflineSignInGateway(sessionRepository, NoMockLatency, store)
        gateway.signIn()
        sessionRepository.markOnboardingComplete()

        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.Main)

            gateway.signOut()
            assertThat(awaitItem()).isEqualTo(AppRootState.FirstRun)

            gateway.signIn()
            assertThat(awaitItem()).isEqualTo(AppRootState.Main)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(store.read(LOCAL_DATA_KEY)).isEqualTo(LOCAL_DATA_VALUE)
    }

    @Test
    fun rootState_whenSignedInWithoutCompletedOnboarding_isFirstRunNotMain() = runTest {
        sessionRepository.sendAccount(SignInAccount.localAccount)
        sessionRepository.sendOnboardingComplete(false)

        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.FirstRun)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val LOCAL_DATA_KEY = "applications.kept"
        const val LOCAL_DATA_VALUE = "kept"
    }
}
