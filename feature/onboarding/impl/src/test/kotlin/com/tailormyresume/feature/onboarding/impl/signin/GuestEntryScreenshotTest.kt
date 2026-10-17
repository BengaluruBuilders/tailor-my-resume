package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class GuestEntryScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun currentOfferAndDisclosure() {
        composeRule.showSignIn {
            SignInScreen(SignInUiState.GuestOffer(GuestTrialOffer.Available(7)), onContinueWithGoogle = {})
        }
        composeRule.onNodeWithText("Get 7 trial credits").assertIsDisplayed()
        composeRule.captureSignIn("guest_trial_offer")
    }

    @Test fun unavailableKeepsContinueVisible() {
        composeRule.showSignIn {
            SignInScreen(SignInUiState.GuestOffer(GuestTrialOffer.Unavailable()), onContinueWithGoogle = {})
        }
        composeRule.onNodeWithText("Continue as guest").assertIsDisplayed()
        composeRule.captureSignIn("guest_trial_unavailable")
    }

    @Test fun enlargedOfferAllowsScrollingToClaimAndContinue() {
        composeRule.showSignIn(fontScale = 2f) {
            SignInScreen(SignInUiState.GuestOffer(GuestTrialOffer.Available(7)), onContinueWithGoogle = {})
        }
        composeRule.onNodeWithText("Get 7 trial credits").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Continue as guest").performScrollTo().assertIsDisplayed()
        composeRule.captureSignIn("guest_trial_large_font", TmrTestDevices.prototypeLargeFont)
    }
}
