package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.chrome.TmrBottomSheet
import com.tailormyresume.core.screenshot.TmrTestDevice
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

private val noActions = PaywallActions({}, {}, {})

private fun paywallWithFailedSheet(): @Composable () -> Unit = {
    PaywallScreen(paywallState(), noActions)
    TmrBottomSheet(onDismiss = {}, title = "Payment didn't go through") {
        PaymentFailedSheetContent(onTryAgain = {}, onCancel = {})
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaywallScreenshotTests {

    @get:Rule
    val rule = createComposeRule()

    private fun check(name: String, device: TmrTestDevice, content: @Composable () -> Unit, vararg texts: String) {
        rule.setTmrContent(device, content)
        rule.assertTexts(*texts)
        rule.assertNoTruncatedText()
        if (device == NARROW_DEVICE) rule.assertNoWordBrokenAcrossLines()
        rule.capture(name, device)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun guestCreditRecoveryUsesExistingWalletAccess() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            PaywallScreen(paywallState().copy(canPurchase = false), noActions)
        }
        rule.assertTexts("Guest credits", "View credits")
        rule.capture("guest_credit_recovery", TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun outOfCreditsPrototype() = check(
        "credits_paywall_outOfCredits",
        TmrTestDevices.prototype,
        { PaywallScreen(paywallState(), noActions) },
        "0 credits left",
        "Buy 15 credits ·\u00A0₹449",
    )

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun buyingPrototype() = check(
        "credits_paywall_buying",
        TmrTestDevices.prototype,
        { PaywallScreen(paywallState(busy = true), noActions) },
        "Processing…",
    )

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun paymentFailedPrototype() = check(
        "credits_paywall_paymentFailed",
        TmrTestDevices.prototype,
        paywallWithFailedSheet(),
        "Payment didn't\ngo through",
        "Try again",
        "Cancel",
    )

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun outOfCreditsNarrow() = check(
        "credits_paywall_outOfCredits",
        NARROW_DEVICE,
        { PaywallScreen(paywallState(), noActions) },
        "0 credits left",
    )

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun paymentFailedNarrow() = check(
        "credits_paywall_paymentFailed",
        NARROW_DEVICE,
        paywallWithFailedSheet(),
        "Payment didn't\ngo through",
        "Try again",
    )
}
