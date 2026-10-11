package com.tailormyresume.feature.settings.impl.navigation

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHostState
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import org.robolectric.Shadows.shadowOf

internal class EntryHarness(
    private val composeRule: AndroidComposeTestRule<*, *>,
    stack: List<NavKey>,
) {
    val state = NavigationState(NavBackStack<NavKey>(*stack.toTypedArray()))
    val navigator = Navigator(state)
    val sheetHost = TmrSheetHostState()
    val toast = TmrToastState()
    private val provider = entryProvider { settingsEntry(navigator) }

    fun show() {
        val shown = state.stack.last()
        composeRule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalTmrSheetHost provides sheetHost, LocalTmrToast provides toast) {
                    provider(shown).Content()
                    TmrSheetHost(sheetHost)
                    TmrToastHost(toast)
                }
            }
        }
        composeRule.waitForIdle()
    }

    fun waitFor(condition: () -> Boolean) {
        composeRule.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
