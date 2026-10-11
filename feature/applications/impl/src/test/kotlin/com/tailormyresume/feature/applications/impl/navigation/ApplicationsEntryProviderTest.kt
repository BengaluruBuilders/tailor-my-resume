package com.tailormyresume.feature.applications.impl.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private object ApplicationsTab : NavKey

private fun navigatorFor(key: NavKey): Navigator =
    Navigator(
        NavigationState(
            startKey = ApplicationsTab,
            topLevelStack = NavBackStack(ApplicationsTab),
            subStacks = listOf(ApplicationsTab, key).associateWith { NavBackStack(it) },
        ),
    )

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class ApplicationsEntryProviderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun applicationsEntryRendersNavKeyClassName() {
        val key = ApplicationsNavKey()
        val navigator = navigatorFor(key)
        val provider: (NavKey) -> NavEntry<NavKey> =
            entryProvider {
                applicationsEntry(navigator)
                applicationDetailEntry(navigator)
            }

        composeRule.setContent {
            TmrTheme {
                provider(key).Content()
            }
        }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }

    @Test
    fun applicationDetailEntryRendersNavKeyClassName() {
        val key = ApplicationDetailNavKey(applicationId = "app-1")
        val navigator = navigatorFor(key)
        val provider: (NavKey) -> NavEntry<NavKey> =
            entryProvider {
                applicationsEntry(navigator)
                applicationDetailEntry(navigator)
            }

        composeRule.setContent {
            TmrTheme {
                provider(key).Content()
            }
        }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }
}
