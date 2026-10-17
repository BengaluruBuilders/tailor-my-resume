package com.tailormyresume.feature.settings.impl.referrals

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.ReferralMessage
import com.tailormyresume.core.domain.ReferralUiState
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ReferralsInputPrivacyTest {
    @get:Rule val rule = createComposeRule()

    @Test fun configuredLinkExposesShareAndLinkContinueUsesExistingAction() {
        var shares = 0
        var continues = 0
        val actions = ReferralActions({}, {}, { _, _ -> }, {}, { continues++ }, {}, {}, {}, share = { shares++ })
        val code = "tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567"
        rule.setContent {
            TmrTheme {
                ReferralsScreen(ReferralUiState.Content(code = code, shareUrl = "https://links.example.invalid/r/$code", candidate = code, candidateSource = "LINK", reward = 13, offerVersion = "v2", canAttach = true), actions)
            }
        }
        rule.onNodeWithText("Share referral link").performScrollTo().performClick()
        rule.onNodeWithText("Continue referral").performScrollTo().performClick()
        assertThat(shares).isEqualTo(1)
        assertThat(continues).isEqualTo(1)
    }

    @Test
    fun readOnlyTransitionAndSavedStateRestorationCannotRestoreTypedOrSavedCode() {
        val state = mutableStateOf<ReferralUiState>(ReferralUiState.Content(candidate = "synthetic-saved-code", canAttach = true))
        val actions = ReferralActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {})
        val restoration = StateRestorationTester(rule)
        restoration.setContent { TmrTheme { ReferralsScreen(state.value, actions) } }
        rule.onNodeWithContentDescription("Referral code").performScrollTo().performTextInput("synthetic-unsaved-code")
        rule.runOnIdle { state.value = ReferralUiState.Content(message = ReferralMessage.ReadOnly) }
        rule.onNodeWithContentDescription("Referral code").assertDoesNotExist()
        rule.onNodeWithText("synthetic-saved-code").assertDoesNotExist()
        rule.onNodeWithText("synthetic-unsaved-code").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Referral code").assertDoesNotExist()
        rule.onNodeWithText("synthetic-saved-code").assertDoesNotExist()
        rule.onNodeWithText("synthetic-unsaved-code").assertDoesNotExist()
        rule.runOnIdle { state.value = ReferralUiState.Content() }
        rule.onNodeWithContentDescription("Referral code").performScrollTo().assertTextEquals("")
    }

    @Test
    fun nonFixtureReturnedCodeIsCopiedWithoutNamespaceRewrite() {
        val code = "resume-demo.preview-2.abcdefghijklmnopqrstuvwxyz234567"
        var copied: String? = null
        val actions = ReferralActions({}, {}, { _, _ -> }, {}, {}, {}, {}, copy = { copied = it })
        rule.setContent { TmrTheme { ReferralsScreen(ReferralUiState.Content(code = code, attached = true), actions) } }
        rule.onNodeWithText("Copy code").performScrollTo().performClick()
        assertThat(copied).isEqualTo(code)
    }
}
