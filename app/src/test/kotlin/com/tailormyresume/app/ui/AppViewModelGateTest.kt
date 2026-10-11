package com.tailormyresume.app.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.offline.OfflineSignInGateway
import com.tailormyresume.core.domain.onboarding.ObserveStartDestinationUseCase
import com.tailormyresume.core.domain.onboarding.StartDestination
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.sampleProfile
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class AppViewModelGateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val sessionRepository = TestSessionRepository()
    private val profileRepository = TestProfileRepository()
    private val credits = CountingCredits()

    private class CountingCredits(private val failure: Throwable? = null) :
        CreditsRepository by TestCreditsRepository() {
        var refreshes = 0

        override suspend fun refresh() {
            refreshes++
            failure?.let { throw it }
        }
    }

    private fun viewModel(wallet: CreditsRepository = credits) = AppViewModel(
        sessionRepository,
        ObserveStartDestinationUseCase(sessionRepository, profileRepository),
        wallet,
    )

    private val first = SignInAccount("account-1", "First", "first@example.com")
    private val second = SignInAccount("account-2", "Second", "second@example.com")

    @Test
    fun rootState_beforeFirstDestination_isLoadingNeverSignIn() = runTest {
        sessionRepository.sendAccount(first)
        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, first.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_signedOut_startsAtSignIn() = runTest {
        viewModel().rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Loading)
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.SignIn, null))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_signedInOnboardingIncomplete_startsAtUpload() = runTest {
        sessionRepository.sendAccount(first)
        sessionRepository.sendOnboardingComplete(false)
        viewModel().rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, first.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_signedInReviewedOrComplete_startsAtApplications() = runTest {
        sessionRepository.sendAccount(first)
        sessionRepository.sendOnboardingComplete(true)
        viewModel().rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, first.id))
            cancelAndIgnoreRemainingEvents()
        }
        profileRepository.sendProfile(sampleProfile.copy(reviewedAt = Instant.parse("2026-10-02T09:30:00Z")))
        sessionRepository.sendOnboardingComplete(false)
        viewModel().rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, first.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_profileReviewedAndOnboardingCompleteAfterStart_doesNotReEmit() = runTest {
        sessionRepository.sendAccount(first)
        viewModel().rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, first.id))

            profileRepository.sendProfile(sampleProfile.copy(reviewedAt = Instant.parse("2026-10-02T09:30:00Z")))
            sessionRepository.markOnboardingComplete()

            expectNoEvents()
        }
    }

    @Test
    fun rootState_signOutThenDifferentAccount_reEmitsEachTime() = runTest {
        sessionRepository.sendAccount(first)
        sessionRepository.sendOnboardingComplete(true)
        viewModel().rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, first.id))

            sessionRepository.signOut()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.SignIn, null))

            sessionRepository.saveAccount(second)
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, second.id))

            sessionRepository.saveAccount(first)
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, first.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_sessionExpiryOfSignedInAccount_returnsToSignIn() = runTest {
        val gateway = OfflineSignInGateway(sessionRepository, NoMockLatency, TestMockStateStore())
        gateway.signIn()
        sessionRepository.markOnboardingComplete()
        viewModel().rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, SignInAccount.localAccount.id))

            gateway.signOut()

            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.SignIn, null))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rootState_sameAccountSignInAfterSignOut_reRootsWithoutWipe() = runTest {
        val store = TestMockStateStore()
        store.write("applications.kept", "kept")
        val gateway = OfflineSignInGateway(sessionRepository, NoMockLatency, store)
        gateway.signIn()
        sessionRepository.markOnboardingComplete()
        viewModel().rootState.test {
            awaitItem()
            awaitItem()
            gateway.signOut()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.SignIn, null))

            gateway.signIn()

            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Applications, SignInAccount.localAccount.id))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(store.read("applications.kept")).isEqualTo("kept")
    }

    @Test
    fun signIn_refreshesWalletOnce() = runTest {
        sessionRepository.sendAccount(first)
        viewModel().rootState.test {
            awaitItem()
            awaitItem()
            sessionRepository.markOnboardingComplete()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(credits.refreshes).isEqualTo(1)
    }

    @Test
    fun signedOut_neverRefreshesWallet() = runTest {
        viewModel().rootState.test {
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(credits.refreshes).isEqualTo(0)
    }

    @Test
    fun signIn_whenWalletRefreshThrows_stillEmitsReady() = runTest {
        sessionRepository.sendAccount(first)
        val failing = CountingCredits(failure = IllegalStateException("offline"))
        viewModel(failing).rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, first.id))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(failing.refreshes).isEqualTo(1)
    }

    @Test
    fun signIn_whenWalletRefreshIsCancelled_cancellationIsNotSwallowed() = runTest {
        sessionRepository.sendAccount(first)
        val cancelling = CountingCredits(failure = CancellationException("cancelled"))
        viewModel(cancelling).rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, first.id))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(cancelling.refreshes).isEqualTo(1)
    }

    @Test
    fun resubscribeAfterTimeout_sameAccount_doesNotRefreshOrFlashLoading() = runTest {
        sessionRepository.sendAccount(first)
        val viewModel = viewModel()
        val ready = AppRootState.Ready(StartDestination.Upload, first.id)
        viewModel.rootState.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(ready)
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(6_000)
        runCurrent()

        viewModel.rootState.test {
            assertThat(awaitItem()).isEqualTo(ready)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(credits.refreshes).isEqualTo(1)
    }

    @Test
    fun resubscribeAfterTimeout_differentAccount_refreshesAgain() = runTest {
        sessionRepository.sendAccount(first)
        val viewModel = viewModel()
        viewModel.rootState.test {
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(6_000)
        runCurrent()
        sessionRepository.sendAccount(second)

        viewModel.rootState.test {
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, first.id))
            assertThat(awaitItem()).isEqualTo(AppRootState.Ready(StartDestination.Upload, second.id))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(credits.refreshes).isEqualTo(2)
    }
}
