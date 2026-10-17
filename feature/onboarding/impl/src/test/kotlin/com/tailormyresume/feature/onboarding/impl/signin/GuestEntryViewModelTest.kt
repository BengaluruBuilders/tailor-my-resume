package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
import com.tailormyresume.core.testing.gateway.TestGuestTrialGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GuestEntryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val guests = TestGuestTrialGateway()
    private val subject = SignInViewModel(TestSignInGateway(), guests)

    @Test fun offerUsesTheCurrentServerAmountAndChangedOffersRequireAnotherTap() = runTest {
        guests.offer = GuestTrialOffer.Available(9)
        guests.result = GuestTrialResult.Refresh(GuestTrialOffer.Available(4, changed = true))
        subject.onTryGuest()
        assertThat(subject.uiState.value).isEqualTo(SignInUiState.GuestOffer(GuestTrialOffer.Available(9)))
        subject.onClaimTrial()
        assertThat(subject.uiState.value).isEqualTo(SignInUiState.GuestOffer(GuestTrialOffer.Available(4, changed = true)))
        assertThat(guests.claims).isEqualTo(1)
        assertThat(guests.continues).isEqualTo(0)
    }

    @Test fun duplicateTapsCannotSubmitParallelClaimsOrSwitchToGoogle() = runTest {
        val reply = CompletableDeferred<Unit>()
        guests.beforeClaim = { reply.await() }
        subject.onTryGuest()
        subject.onClaimTrial()
        subject.onClaimTrial()
        subject.onContinueWithGoogle()
        subject.onContinueGuest()
        assertThat(guests.claims).isEqualTo(1)
        assertThat(guests.continues).isEqualTo(0)
        reply.complete(Unit)
        assertThat(subject.uiState.value).isEqualTo(SignInUiState.GuestResult(GuestTrialResult.Granted(2)))
        subject.onContinueGuest()
        assertThat(guests.continues).isEqualTo(1)
    }

    @Test fun unavailableTrialCanContinueAndResumeRefreshesTheOffer() = runTest {
        guests.offer = GuestTrialOffer.Unavailable()
        subject.onTryGuest()
        subject.onContinueGuest()
        assertThat(guests.continues).isEqualTo(1)
        guests.offer = GuestTrialOffer.Available(3)
        subject.onResume()
        assertThat(guests.prepares).isEqualTo(2)
        assertThat(subject.uiState.value).isEqualTo(SignInUiState.GuestOffer(GuestTrialOffer.Available(3)))
    }
}
