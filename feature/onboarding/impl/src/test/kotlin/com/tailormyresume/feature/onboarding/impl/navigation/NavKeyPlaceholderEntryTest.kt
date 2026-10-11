package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ImportResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

private object Tab : NavKey

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavKeyPlaceholderEntryTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val navigator = Navigator(
        NavigationState(
            startKey = Tab,
            topLevelStack = NavBackStack(Tab),
            subStacks = mapOf(Tab to NavBackStack(Tab)),
        ),
    )

    @Test
    fun signInNavKeyShowsItsName() {
        val key: NavKey = SignInNavKey()
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { onboardingEntry(navigator) }

        composeRule.setContent { provider(key).Content() }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }

    @Test
    fun importResumeNavKeyShowsItsName() {
        val key: NavKey = ImportResumeNavKey()
        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider { onboardingEntry(navigator) }

        composeRule.setContent { provider(key).Content() }

        composeRule.onNodeWithText(checkNotNull(key::class.simpleName)).assertIsDisplayed()
    }
}
