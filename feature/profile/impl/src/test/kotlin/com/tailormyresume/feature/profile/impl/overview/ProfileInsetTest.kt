package com.tailormyresume.feature.profile.impl.overview

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileInsetTest {

    @get:Rule
    val rule = createComposeRule()

    private val opened = mutableListOf<ProfileTarget>()

    private fun show() {
        rule.setContent {
            TmrTheme {
                ProfileScreen(
                    state = ProfileUiState.Loading,
                    onOpen = { opened += it },
                    topInset = WindowInsets(top = 48.dp),
                )
            }
        }
    }

    @Test
    fun settingsGear_startsBelowStatusBarInset() {
        show()

        val top = rule.onNodeWithText("Settings").fetchSemanticsNode().boundsInRoot.top
        val inset = with(rule.density) { 48.dp.toPx() }

        assertThat(top).isAtLeast(inset)
    }

    @Test
    fun settingsGearTap_opensSettings() {
        show()

        rule.onNodeWithText("Settings").performClick()

        assertThat(opened).containsExactly(ProfileTarget.SETTINGS)
    }
}
