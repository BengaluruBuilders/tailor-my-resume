package com.tailormyresume.feature.analysis.impl.navigation

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
import com.tailormyresume.feature.analysis.api.navigation.AnalysisNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private object AnalysisTab : NavKey

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class AnalysisEntryProviderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun analysisEntryRendersNavKeyClassName() {
        val navigator = Navigator(
            NavigationState(
                startKey = AnalysisTab,
                topLevelStack = NavBackStack(AnalysisTab),
                subStacks = mapOf(AnalysisTab to NavBackStack(AnalysisTab)),
            ),
        )
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { analysisEntry(navigator) }
        val key = AnalysisNavKey()

        composeRule.setContent {
            TmrTheme(darkTheme = false) {
                provider(key).Content()
            }
        }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }
}
