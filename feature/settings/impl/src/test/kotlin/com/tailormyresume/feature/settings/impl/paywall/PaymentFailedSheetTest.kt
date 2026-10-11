package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.impl.assertTexts
import com.tailormyresume.feature.settings.impl.setTmrContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class PaymentFailedSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private val taps = mutableListOf<String>()

    @Test
    fun copyAndButtons() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            Column {
                PaymentFailedSheetContent(onTryAgain = { taps += "again" }, onCancel = { taps += "cancel" })
            }
        }

        rule.assertTexts(
            "!",
            "Payment didn't go through",
            "You weren't charged. Try again, or pick another payment method.",
        )
        rule.onNodeWithText("Try again").performClick()
        rule.onNodeWithText("Cancel").performClick()

        assertThat(taps).containsExactly("again", "cancel").inOrder()
    }

    @Test
    fun unconfirmedCopyMakesNoClaimAboutCharging() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            Column {
                PaymentFailedSheetContent(onTryAgain = {}, onCancel = {}, unconfirmed = true)
            }
        }

        rule.assertTexts(
            "Payment didn't go through",
            "We haven't confirmed your payment yet. Your credits will appear once it is confirmed.",
        )
        assertThat(rule.onAllNodesWithText("charged", substring = true).fetchSemanticsNodes()).isEmpty()
    }
}
