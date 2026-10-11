package com.tailormyresume.feature.settings.impl.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHostState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.rememberNavigationState
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.settings.impl.delete.DeleteFixture
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsDeletePreviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun previewOpensTheSheetWithoutDeletingAndKeepClosesIt() {
        val host = TmrSheetHostState()
        val settings = SettingsFixture().viewModel()
        val deletion = DeleteFixture()
        val deleteViewModel = deletion.viewModel()
        rule.setContent {
            TmrTheme {
                val state = rememberNavigationState(SettingsNavKey())
                val navigator = remember(state) { Navigator(state) }
                CompositionLocalProvider(LocalTmrSheetHost provides host) {
                    SettingsRoute(navigator, viewModel = settings, deleteViewModel = deleteViewModel, showDeleteAccount = true)
                    TmrSheetHost(host)
                }
            }
        }
        rule.onNodeWithText("Type DELETE to confirm").assertIsDisplayed()
        assertThat(deletion.deleter.deleteCalls).isEqualTo(0)
        rule.onNodeWithText("Keep my account").performClick()
        rule.onNodeWithText("Type DELETE to confirm").assertDoesNotExist()
        assertThat(deletion.deleter.deleteCalls).isEqualTo(0)
    }
}
