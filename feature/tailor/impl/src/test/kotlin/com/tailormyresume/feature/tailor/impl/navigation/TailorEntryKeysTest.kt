package com.tailormyresume.feature.tailor.impl.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailorFailedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import com.tailormyresume.feature.tailor.impl.navigation.tailorEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class TailorEntryKeysTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(TailoringNavKey("app-1", "run-1"))))

    private val provider: (NavKey) -> NavEntry<NavKey> =
        entryProvider {
            tailorEntry(navigator)
        }

    private val keys: List<NavKey> =
        listOf(
            EditResumeNavKey("app-1"),
            ExportedNavKey("app-1"),
        )

    private val routedKeys: List<NavKey> =
        listOf(
            TailoringNavKey("app-1", "run-1"),
            TailorFailedNavKey("app-1", "run-1"),
            TailoredNavKey("app-1"),
        )

    @Test
    fun routedKeysResolveToEntries() {
        routedKeys.forEach { key -> assertNotNull("${key::class.simpleName} entry", provider(key)) }
    }

    @Test
    fun everyKeyResolvesAndShowsNameInTextColour() {
        var current: NavKey by mutableStateOf(keys.first())
        var textColour = Color.Unspecified
        composeRule.setContent {
            TmrTheme {
                textColour = TmrTheme.colors.text
                provider(current).Content()
            }
        }

        keys.forEach { key ->
            composeRule.runOnIdle { current = key }
            composeRule.waitForIdle()

            val name = checkNotNull(key::class.simpleName)
            composeRule.onNodeWithText(name).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(name).fetchSemanticsNode().config
                .getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            assertTrue("$name has a text layout", layouts.isNotEmpty())
            assertEquals("$name text colour", textColour, layouts.first().layoutInput.style.color)
        }
    }
}
