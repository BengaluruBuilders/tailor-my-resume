package com.tailormyresume.feature.profile.impl.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.profile.api.navigation.FactEditorNavKey
import com.tailormyresume.feature.profile.api.navigation.FactEvidenceNavKey
import com.tailormyresume.feature.profile.api.navigation.GuidedProfileFormNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private object Tab : NavKey

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class NavKeyPlaceholderEntryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = Navigator(
        NavigationState(
            startKey = Tab,
            topLevelStack = NavBackStack(Tab),
            subStacks = mapOf(Tab to NavBackStack(Tab)),
        ),
    )

    @Test
    fun profileNavKeyRendersKeySimpleName() {
        val key: NavKey = ProfileNavKey()
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { profileEntry(navigator) }

        composeRule.setContent { provider(key).Content() }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }

    @Test
    fun factEditorNavKeyRendersKeySimpleName() {
        val key: NavKey = FactEditorNavKey(entryId = null)
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { profileEntry(navigator) }

        composeRule.setContent { provider(key).Content() }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }

    @Test
    fun guidedProfileFormNavKeyRendersKeySimpleName() {
        val key: NavKey = GuidedProfileFormNavKey()
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { profileEntry(navigator) }

        composeRule.setContent { provider(key).Content() }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }

    @Test
    fun factEvidenceNavKeyRendersKeySimpleName() {
        val key: NavKey = FactEvidenceNavKey()
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { profileEntry(navigator) }

        composeRule.setContent { provider(key).Content() }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }
}
