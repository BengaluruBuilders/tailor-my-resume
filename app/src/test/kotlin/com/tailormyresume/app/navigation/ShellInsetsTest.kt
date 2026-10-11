package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ui.TmrShell
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.rememberNavigationState
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShellInsetsTest {
    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun signInKeepsItsFullBleedBackground() = assertClearsSystemBars(DefaultSignInNavKey, fullBleed = true)

    @Test fun applicationsClearsSystemBars() = assertClearsSystemBars(DefaultApplicationsNavKey)

    @Test fun profileClearsSystemBars() = assertClearsSystemBars(DefaultProfileNavKey)

    private fun assertClearsSystemBars(start: NavKey, fullBleed: Boolean = false) {
        rule.setContent {
            TmrTheme {
                val state = rememberNavigationState(start)
                val navigator = remember(state) { Navigator(state) }
                TmrShell(
                    state,
                    navigator,
                    systemBarInsets = WindowInsets(top = 48.dp, bottom = 32.dp),
                    entryProvider = { key -> NavEntry(key) { Box(Modifier.fillMaxSize().testTag("screen")) } },
                )
            }
        }
        val root = rule.onRoot().getBoundsInRoot()
        val screen = rule.onNodeWithTag("screen").getBoundsInRoot()
        assertThat((screen.top - root.top).value).isWithin(0.5f).of(if (fullBleed) 0f else 48f)
        assertThat((root.bottom - screen.bottom).value).isAtLeast(if (fullBleed) 0f else 32f)
    }
}
