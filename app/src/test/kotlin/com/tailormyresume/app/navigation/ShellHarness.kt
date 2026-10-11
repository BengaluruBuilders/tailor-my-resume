package com.tailormyresume.app.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import com.tailormyresume.app.ui.TmrShell
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.rememberNavigationState
import org.robolectric.Shadows.shadowOf

private val PLACEHOLDER_ENTRIES: (NavKey) -> NavEntry<NavKey> = entryProvider(
    fallback = { key ->
        NavEntry(key) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = key::class.simpleName.orEmpty(),
                    style = TmrTheme.typography.body,
                    color = TmrTheme.colors.text,
                )
            }
        }
    },
) {}

private val STEP_DESCRIPTIONS = listOf("Step 1 of 3, Profile", "Step 2 of 3, Job", "Step 3 of 3, Tailor")

internal class ShellHarness(private val rule: ComposeContentTestRule) {

    var hasHome: Boolean = false

    lateinit var navigator: Navigator
        private set

    lateinit var state: NavigationState
        private set

    val stack: List<NavKey> get() = state.stack.toList()

    fun show(
        start: NavKey,
        entries: ((Navigator) -> (NavKey) -> NavEntry<NavKey>)? = null,
        fontScale: Float = 1f,
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TmrTheme {
                    val navigationState = rememberNavigationState(start)
                    val shellNavigator = remember(navigationState) { shellNavigator(navigationState) { hasHome } }
                    state = navigationState
                    navigator = shellNavigator
                    TmrShell(
                        navigationState,
                        shellNavigator,
                        entryProvider = entries?.invoke(shellNavigator) ?: PLACEHOLDER_ENTRIES,
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    fun go(vararg keys: NavKey) {
        rule.runOnIdle { keys.forEach(navigator::navigate) }
        rule.waitForIdle()
    }

    fun root(key: NavKey) {
        rule.runOnIdle { navigator.root(key) }
        rule.waitForIdle()
    }

    fun clickBack() = click("Back")

    fun clickClose() = click("Close")

    fun click(description: String) {
        rule.onNode(hasContentDescription(description)).performClick()
        rule.waitForIdle()
    }

    fun assertChrome(row: ChromeRow, leading: TmrTopBarLeading = row.leading) {
        val label = row.key::class.simpleName
        val stepNodes = rule.onAllNodes(hasContentDescription("Step ", substring = true))
        if (row.step == null) {
            stepNodes.assertCountEquals(0)
        } else {
            rule.onAllNodes(hasContentDescription(STEP_DESCRIPTIONS[row.step - 1])).assertCountEquals(1)
        }
        rule.onAllNodes(hasContentDescription("Back")).assertCountEquals(if (leading == TmrTopBarLeading.Back) 1 else 0)
        rule.onAllNodes(hasContentDescription("Close")).assertCountEquals(if (leading == TmrTopBarLeading.Close) 1 else 0)
        listOf("Resume", "Application", "Experience", "Settings").filter { it != row.title }.forEach { other ->
            rule.onAllNodesWithText(other, ignoreCase = true).assertCountEquals(0)
        }
        row.title?.let { rule.onNodeWithText(it, ignoreCase = true).assertExists("$label title") }
        listOf("Save", "Done").filter { it != row.action }.forEach { other ->
            rule.onAllNodesWithText(other).assertCountEquals(0)
        }
        row.action?.let { rule.onNodeWithText(it).assertExists("$label action") }
    }
}

internal fun registerComposeActivity() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    shadowOf(application.packageManager)
        .addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
}
