package com.tailormyresume.feature.settings.impl.referrals

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ReferralMessage
import com.tailormyresume.core.domain.ReferralUiState
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.impl.NARROW_DEVICE
import com.tailormyresume.feature.settings.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.settings.impl.assertNoTruncatedText
import com.tailormyresume.feature.settings.impl.capture
import com.tailormyresume.feature.settings.impl.setTmrContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferralsScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private var copies = 0
    private val actions = ReferralActions({}, {}, { _, _ -> }, {}, {}, {}, {}) { copies++ }
    @Test @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun serverOfferAndCopyCodeUseActualAmount() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            ReferralsScreen(ReferralUiState.Content(code = "tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567", reward = 13, creditedCount = 2, earnedCredits = 26, attached = true), actions)
        }
        rule.onNodeWithText("Copy code").performClick()
        assertThat(copies).isEqualTo(1)
        rule.assertNoTruncatedText()
        rule.capture("referrals_inviter", TmrTestDevices.prototype)
    }
    @Test @Config(qualifiers = NARROW_QUALIFIERS)
    fun unavailableLargeTextHasNoRewardPromiseAndKeepsRefreshReachable() {
        rule.setTmrContent(NARROW_DEVICE) { ReferralsScreen(ReferralUiState.Content(message = ReferralMessage.ReadOnly), actions) }
        rule.assertNoTruncatedText()
        rule.capture("referrals_read_only", NARROW_DEVICE)
    }
}
