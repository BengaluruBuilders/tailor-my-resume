package com.tailormyresume.feature.settings.impl.credits

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.testing.gateway.TestGuestTrialGateway
import com.tailormyresume.core.testing.gateway.TestAccountWallet
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GuestCreditsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val credits = TestCreditsRepository()
    private val guests = TestGuestTrialGateway()
    private val uid = object : FirebaseUidProvider {
        override fun uid() = "guest"
        override fun isAnonymous() = true
    }
    private fun subject() = CreditsViewModel(credits, AccountCreditBalance(TestAccountWallet(), credits), TestApplicationRepository(), uid, guests)

    @Test fun guestCanRetryUnavailableOfferWithoutLosingBalanceOrOpeningPurchases() = runTest {
        guests.offer = GuestTrialOffer.Unavailable()
        val subject = subject()
        subject.uiState.test {
            awaitItem()
            subject.onCheckTrial()
            val unavailable = expectMostRecentItem() as CreditsUiState.Content
            assertThat(unavailable.isGuest).isTrue()
            assertThat(unavailable.trial).isEqualTo(CreditsTrialState.Offer(GuestTrialOffer.Unavailable()))
            guests.offer = GuestTrialOffer.Available(8)
            subject.onResume()
            assertThat((expectMostRecentItem() as CreditsUiState.Content).trial).isEqualTo(CreditsTrialState.Offer(GuestTrialOffer.Available(8)))
        }
    }

    @Test fun parallelClaimTapsDoNotDuplicateAndChangedPolicyNeedsAnotherClaim() = runTest {
        val completed = CompletableDeferred<Unit>()
        guests.beforeClaim = { completed.await() }
        guests.result = GuestTrialResult.Refresh(GuestTrialOffer.Available(3, changed = true))
        val subject = subject()
        subject.uiState.test {
            awaitItem()
            subject.onCheckTrial()
            subject.onClaimTrial()
            subject.onClaimTrial()
            assertThat(guests.claims).isEqualTo(1)
            completed.complete(Unit)
            val content = expectMostRecentItem() as CreditsUiState.Content
            assertThat(content.trial).isEqualTo(CreditsTrialState.Offer(GuestTrialOffer.Available(3, changed = true)))
        }
    }
}
