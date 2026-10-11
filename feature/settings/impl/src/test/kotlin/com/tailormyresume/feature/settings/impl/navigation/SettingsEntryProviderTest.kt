package com.tailormyresume.feature.settings.impl.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.api.navigation.DeleteAccountNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private object SettingsTab : NavKey

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class SettingsEntryProviderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsEntryRendersNavKeyClassNameForEachSettingsKey() {
        val state = NavigationState(
            startKey = SettingsTab,
            topLevelStack = NavBackStack(SettingsTab),
            subStacks = mapOf(SettingsTab to NavBackStack(SettingsTab)),
        )
        val navigator = Navigator(state)
        val provider = entryProvider { settingsEntry(navigator) }
        val keys = listOf<NavKey>(
            SettingsNavKey(),
            DeleteAccountNavKey(),
        )

        composeRule.setContent {
            TmrTheme {
                Column {
                    keys.forEach { navKey ->
                        key(navKey) { provider(navKey).Content() }
                    }
                }
            }
        }

        keys.forEach { navKey ->
            composeRule.onNodeWithText(checkNotNull(navKey::class.simpleName)).assertIsDisplayed()
        }
    }
}
