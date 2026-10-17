package com.tailormyresume.feature.settings.impl.credits

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.impl.NARROW_DEVICE
import com.tailormyresume.feature.settings.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.settings.impl.assertNoTruncatedText
import com.tailormyresume.feature.settings.impl.assertNoWordBrokenAcrossLines
import com.tailormyresume.feature.settings.impl.assertTexts
import com.tailormyresume.feature.settings.impl.capture
import com.tailormyresume.feature.settings.impl.setTmrContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CreditsScreenshotTests {

    @get:Rule
    val rule = createComposeRule()

    private var inviteTaps = 0

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun guestTrialCanBeClaimedWithoutPurchases() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(
                CreditsUiState.Content(
                    credits = 0,
                    history = emptyList(),
                    isGuest = true,
                    trial = CreditsTrialState.Offer(com.tailormyresume.core.domain.GuestTrialOffer.Available(7)),
                ),
                onReferrals = {},
            )
        }
        rule.onNodeWithText("Get 7 trial credits").performClick()
        rule.capture("guest_credits_offer", TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun creditsReturningPrototype() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(returningCreditsState(), onReferrals = { inviteTaps += 1 })
        }

        rule.assertTexts(
            "Credits",
            "3",
            "credits left. They never expire.",
            "Invite friends",
            "History",
            "Free first resume",
            "Welcome · 2 Oct",
            "Bought 5 credits",
            "5 Oct",
            "Associate Analyst",
            "Northwind GCC · 8 Oct",
            "+5",
            "−1",
        )
        rule.assertNoTruncatedText()
        rule.capture("credits_returning", TmrTestDevices.prototype)
        rule.onNodeWithText("Invite friends").performClick()
        assertThat(inviteTaps).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun creditsReturningNarrow() {
        rule.setTmrContent(NARROW_DEVICE) { CreditsScreen(returningCreditsState(), onReferrals = {}) }

        rule.assertNoTruncatedText()
        rule.assertNoWordBrokenAcrossLines()
        rule.capture("credits_returning", NARROW_DEVICE)
        rule.assertNoTruncatedText()
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun creditsWithNoHistoryOffersEarning() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(CreditsUiState.Content(credits = 0, history = emptyList()), onReferrals = {})
        }

        rule.assertTexts("0", "Invite friends")
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun singleCreditCaptionIsSingular() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(CreditsUiState.Content(credits = 1, history = emptyList()), onReferrals = {})
        }

        rule.assertTexts("1", "credit left. It never expires.")
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun twoDigitBalanceSitsBesideItsCaption() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(CreditsUiState.Content(credits = 30, history = emptyList()), onReferrals = {})
        }

        val number = rule.onNodeWithText("30").fetchSemanticsNode().boundsInRoot
        val caption = rule.onNodeWithText("credits left. They never expire.").fetchSemanticsNode().boundsInRoot
        assertThat(caption.left).isAtLeast(number.right)
        assertThat(caption.top).isLessThan(number.bottom)
        rule.capture("credits_twoDigit", TmrTestDevices.prototype)
    }
}
