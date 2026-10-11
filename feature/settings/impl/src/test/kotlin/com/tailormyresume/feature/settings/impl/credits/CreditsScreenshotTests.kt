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

    private var buyMoreTaps = 0

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun creditsReturningPrototype() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(returningCreditsState(), onBuyMore = { buyMoreTaps += 1 })
        }

        rule.assertTexts(
            "Credits",
            "3",
            "credits left. They never expire.",
            "Buy more",
            "History",
            "Free first resume",
            "Welcome · 2 Oct",
            "Bought 5 credits",
            "₹199 · 5 Oct",
            "Associate Analyst",
            "Northwind GCC · 8 Oct",
            "+5",
            "−1",
            "Receipts go to your Google email",
        )
        rule.assertNoTruncatedText()
        rule.capture("credits_returning", TmrTestDevices.prototype)
        rule.onNodeWithText("Buy more").performClick()
        assertThat(buyMoreTaps).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun creditsReturningNarrow() {
        rule.setTmrContent(NARROW_DEVICE) { CreditsScreen(returningCreditsState(), onBuyMore = {}) }

        rule.assertNoTruncatedText()
        rule.assertNoWordBrokenAcrossLines()
        rule.capture("credits_returning", NARROW_DEVICE)
        rule.onNodeWithTag(CREDITS_LIST_TAG).performScrollToNode(hasText("Receipts go to your Google email"))
        rule.assertTexts("Receipts go to your Google email")
        rule.assertNoTruncatedText()
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun creditsWithNoHistoryStillOffersBuyMore() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            CreditsScreen(CreditsUiState.Content(credits = 0, history = emptyList()), onBuyMore = {})
        }

        rule.assertTexts("0", "Buy more", "Receipts go to your Google email")
    }
}
