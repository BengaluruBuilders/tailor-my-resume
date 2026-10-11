package com.tailormyresume.app.debug

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.rememberNavigationState
import com.tailormyresume.core.navigation.toEntries
import com.tailormyresume.feature.onboarding.api.navigation.ImportResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class NavigationStateRestoreTest {

    init {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application.packageManager)
            .addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @get:Rule
    val composeRule = createComposeRule()

    private val firstKey: NavKey = SignInNavKey(scenario = DebugScenario.defaultValue)
    private val otherKey: NavKey = ImportResumeNavKey(scenario = DebugScenario.defaultValue)
    private var startKey: NavKey = firstKey
    private lateinit var state: NavigationState
    private var entryCount = 0

    @Composable
    private fun Root() {
        state = rememberNavigationState(startKey, setOf(startKey))
        entryCount = state.toEntries { key -> NavEntry(key) {} }.size
    }

    @Test
    fun aStackRestoredForAnotherStartKeyOpensTheNewStartKey() {
        val tester = StateRestorationTester(composeRule)
        tester.setContent { Root() }
        composeRule.waitForIdle()

        startKey = otherKey
        tester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(entryCount).isEqualTo(1)
        assertThat(state.currentKey).isEqualTo(otherKey)
    }

    @Test
    fun aStackRestoredForTheSameKeysIsKept() {
        val tester = StateRestorationTester(composeRule)
        tester.setContent { Root() }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { state.currentSubStack.add(otherKey) }
        composeRule.waitForIdle()

        tester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(state.currentSubStack.toList()).containsExactly(firstKey, otherKey).inOrder()
        assertThat(entryCount).isEqualTo(2)
    }
}
