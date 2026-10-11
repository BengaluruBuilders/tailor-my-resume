package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHostState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.RegisterChromeAction
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

private const val TOAST_TAG = "tmr_chrome_toast"
private const val SHEET_TAG = "tmr_chrome_sheet"
private const val FRAMES_TO_SETTLE = 5

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShellHostsTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shell = ShellHarness(rule)

    private fun provider(
        onSave: () -> Unit = {},
        registerSave: Boolean = false,
        showHosts: Boolean = false,
    ): (Navigator) -> (NavKey) -> NavEntry<NavKey> = { _ ->
        { key ->
            NavEntry(key) {
                val toast = LocalTmrToast.current
                val sheet = LocalTmrSheetHost.current
                LaunchedEffect(key) {
                    if (showHosts) {
                        toast.show("toast one")
                        toast.show("toast two")
                        sheet.show("Sheet A") { Text("sheet body A") }
                        sheet.show("Sheet B") { Text("sheet body B") }
                    }
                }
                if (registerSave && key is EditResumeNavKey) RegisterChromeAction(onSave)
                Column { Text(checkNotNull(key::class.simpleName)) }
            }
        }
    }

    @Test
    fun exactlyOneToastHostAndOneSheetHost() {
        rule.mainClock.autoAdvance = false
        shell.show(DefaultApplicationsNavKey, provider(showHosts = true))
        rule.mainClock.advanceTimeByFrame()

        rule.onAllNodesWithTag(TOAST_TAG).assertCountEquals(1)
        rule.onAllNodesWithTag(SHEET_TAG).assertCountEquals(1)
        rule.onNodeWithText("toast two").assertExists()
        rule.onAllNodesWithText("toast one").assertCountEquals(0)
        rule.onNodeWithText("sheet body B").assertExists()
        rule.onAllNodesWithText("sheet body A").assertCountEquals(0)
    }

    @Test
    fun aRegisteredChromeActionReplacesTheDefaultSave() {
        var saved = 0
        rule.mainClock.autoAdvance = false
        shell.show(DefaultApplicationsNavKey, provider(onSave = { saved += 1 }, registerSave = true))
        shell.go(EditResumeNavKey("app-1"))
        repeat(FRAMES_TO_SETTLE) { rule.mainClock.advanceTimeByFrame() }

        rule.onNodeWithText("Save").performClick()
        rule.mainClock.advanceTimeByFrame()

        assertThat(saved).isEqualTo(1)
        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey, EditResumeNavKey("app-1")).inOrder()
    }

    @Test
    fun theDefaultSavePopsAndToastsChangesSaved() {
        rule.mainClock.autoAdvance = false
        shell.show(DefaultApplicationsNavKey)
        shell.go(EditResumeNavKey("app-1"))
        repeat(FRAMES_TO_SETTLE) { rule.mainClock.advanceTimeByFrame() }

        rule.onNodeWithText("Save").performClick()
        rule.mainClock.advanceTimeByFrame()

        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey)
        rule.onNodeWithText("Changes saved").assertExists()
    }

    @Test
    fun aSheetOpenOnOneScreenIsGoneAfterGo() = assertSheetGoneAfter { it.go(EditResumeNavKey("app-1")) }

    @Test
    fun aSheetOpenOnOneScreenIsGoneAfterBack() = assertSheetGoneAfter { it.navigator.goBack() }

    @Test
    fun aSheetOpenOnOneScreenIsGoneAfterRoot() = assertSheetGoneAfter { it.root(EditResumeNavKey("app-1")) }

    @Test
    fun aSheetOpenOnOneScreenIsGoneAfterReplace() = assertSheetGoneAfter { it.navigator.replace(EditResumeNavKey("app-1")) }

    private fun assertSheetGoneAfter(navigate: (ShellHarness) -> Unit) {
        lateinit var sheetHost: TmrSheetHostState
        shell.show(DefaultApplicationsNavKey, { _ ->
            { key ->
                NavEntry(key) {
                    sheetHost = LocalTmrSheetHost.current
                    Text(checkNotNull(key::class.simpleName))
                }
            }
        })
        shell.go(EditResumeNavKey("app-0"))
        rule.runOnIdle { sheetHost.show("Sheet") { Text("sheet body") } }
        rule.onNodeWithText("sheet body").assertExists()

        rule.runOnIdle { navigate(shell) }
        rule.waitForIdle()

        rule.onAllNodesWithText("sheet body").assertCountEquals(0)
        assertThat(sheetHost.current).isNull()
    }
}
