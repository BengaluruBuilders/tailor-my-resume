package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.offline.MockPackCatalogue
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.impl.assertTexts
import com.tailormyresume.feature.settings.impl.setTmrContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal fun paywallState(
    credits: Int = 0,
    selected: String? = ApplicationPack.APPLICATION_PACK_FIFTEEN,
    busy: Boolean = false,
    packs: List<ApplicationPack> = MockPackCatalogue.all,
) = PaywallUiState(credits = credits, packs = packs, selectedPackId = selected, busy = busy)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class PaywallScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val taps = mutableListOf<String>()
    private val actions = PaywallActions(
        onSelectPack = { taps += "select:$it" },
        onBuy = { taps += "buy" },
        onRestore = { taps += "restore" },
    )

    private fun show(state: PaywallUiState) {
        rule.setTmrContent(TmrTestDevices.prototype) { PaywallScreen(state, actions) }
    }

    @Test
    fun rendersHeroPacksAndBuyLabel() {
        show(paywallState())

        rule.assertTexts(
            "0 credits left",
            "Keep tailoring for every job",
            "1 credit = 1 tailored resume. Credits never expire.",
            "5 credits",
            "15 credits",
            "40 credits",
            "₹40 per resume",
            "₹30 per resume",
            "₹25 per resume",
            "Best value",
            "Buy 15 credits · ₹449",
            "One-time payment · Restore purchases",
        )
    }

    @Test
    fun heroLabelIsPlural() {
        show(paywallState(credits = 1))
        rule.assertTexts("1 credit left")
    }

    @Test
    fun heroLabelCountsSeveral() {
        show(paywallState(credits = 3))
        rule.assertTexts("3 credits left")
    }

    @Test
    fun tappingARowSelectsItAndTheButtonBuys() {
        show(paywallState())

        rule.onNodeWithText("5 credits").performClick()
        rule.onNodeWithText("Buy 15 credits", substring = true).performClick()
        rule.onNodeWithText("Restore purchases", substring = true).performClick()

        assertThat(taps).containsExactly("select:application_pack_5", "buy", "restore").inOrder()
    }

    @Test
    fun buyingShowsProcessing() {
        show(paywallState(busy = true))

        rule.assertTexts("Processing…")
        rule.onNodeWithText("Processing…").performClick()
        assertThat(taps).isEmpty()
    }

    @Test
    fun withoutPricesTheBuyButtonStaysTappableToRetry() {
        show(paywallState(packs = emptyList(), selected = null))

        rule.onNodeWithText("Buy credits").performClick()

        assertThat(taps).containsExactly("buy")
    }
}
