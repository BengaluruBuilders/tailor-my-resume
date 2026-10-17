package com.tailormyresume.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.GoogleAccountState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GoogleAccountPanelTest {
    init {
        com.tailormyresume.app.navigation.registerComposeActivity()
    }

    @get:Rule val rule = createComposeRule()

    @Test fun linkDisclosureShowsCurrentOfferAndClearOnExistingGoogleSurface() {
        var clears = 0
        rule.setContent {
            TmrTheme {
                GoogleAccountPanel(
                    GoogleAccountState.Offer(null, null, false),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    referral = com.tailormyresume.core.domain.ReferralEntryState("tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567", 13, "referral-v2", "2026-10-18T00:00:00Z"),
                    onClearReferral = { clears++ },
                )
            }
        }
        rule.onNodeWithText("An eligible referral may earn the inviter 13 credits. Offer referral-v2. Check availability in the app.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Clear referral code").performScrollTo().performClick()
        assertThat(clears).isEqualTo(1)
    }

    @Test fun independentServerOfferShowsOnlyTheAvailableGrantAndExplicitClaim() {
        var claims = 0
        rule.setContent {
            TmrTheme { panel(GoogleAccountState.Offer(null, 13, true), onClaim = { claims++ }) }
        }
        rule.onNodeWithText("A separate eligible trial offers 13 credits. The device trial check still applies.")
            .assertIsDisplayed()
        rule.onNodeWithText("Add eligible credits").performScrollTo().performClick()
        assertThat(claims).isEqualTo(1)
    }

    @Test fun conflictSelectionIsExplicitAndCancellingDoesNotContinue() {
        var continued: Set<String>? = null
        var cancellations = 0
        rule.setContent {
            TmrTheme {
                panel(
                    GoogleAccountState.Conflict(false, listOf("app-A" to "Role A", "app-B" to "Role B")),
                    onConflict = { _, selected -> continued = selected },
                    onCancel = { cancellations++ },
                )
            }
        }
        rule.onNodeWithText("Keep using this guest account").performScrollTo().performClick()
        assertThat(cancellations).isEqualTo(1)
        assertThat(continued).isNull()
        rule.onNodeWithText("Role B").performScrollTo().performClick()
        rule.onNodeWithText("Switch and keep selected work").performScrollTo().performClick()
        assertThat(continued).containsExactly("app-A")
    }

    @Composable
    private fun panel(
        state: GoogleAccountState,
        onClaim: () -> Unit = {},
        onConflict: (Boolean, Set<String>) -> Unit = { _, _ -> },
        onCancel: () -> Unit = {},
    ) {
        GoogleAccountPanel(state, onClaim, onRetry = {}, onCancel, onClose = {}, onConflict)
    }
}
