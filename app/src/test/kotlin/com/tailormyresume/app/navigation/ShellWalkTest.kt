package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

private const val ID = "app-1"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShellWalkTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shell = ShellHarness(rule)

    private fun rowOf(key: NavKey): ChromeRow = CHROME_TABLE.first { it.key::class == key::class }

    private fun name(key: NavKey): String = checkNotNull(key::class.simpleName)

    @Test
    fun walkAssertsChromeOnEveryScreen() {
        shell.show(SignInNavKey())
        rule.onNodeWithText(name(SignInNavKey())).assertExists()
        shell.assertChrome(rowOf(SignInNavKey()))

        shell.root(UploadNavKey())
        rule.onNodeWithText(name(UploadNavKey())).assertExists()
        shell.assertChrome(rowOf(UploadNavKey()), leading = TmrTopBarLeading.None)

        listOf(
            ReadingNavKey(),
            ReviewProfileNavKey(),
            JobNavKey(),
            JobResultNavKey(ID),
            QuickQuestionNavKey(ID),
            TailoringNavKey(ID, "run-1"),
            TailoredNavKey(ID),
            ExportedNavKey(ID),
        ).forEach { key ->
            shell.go(key)
            rule.onNodeWithText(name(key)).assertExists()
            shell.assertChrome(rowOf(key))
        }

        shell.hasHome = true
        shell.clickClose()
        rule.onNodeWithText(name(DefaultApplicationsNavKey)).assertExists()
        shell.assertChrome(rowOf(DefaultApplicationsNavKey))
    }

    @Test
    fun systemBackMatchesTopBarBack() {
        shell.hasHome = true
        shell.show(UploadNavKey())
        shell.go(ReviewProfileNavKey(), JobNavKey())

        shell.clickBack()
        assertThat(shell.stack).containsExactly(UploadNavKey(), ReviewProfileNavKey()).inOrder()

        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertThat(shell.stack).containsExactly(UploadNavKey())
        rule.onAllNodes(hasContentDescription("Back")).assertCountEquals(0)

        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey)
    }

    @Test
    fun exportedCloseLeavesEmptyStackAtApplications() {
        shell.hasHome = true
        shell.show(SignInNavKey())
        shell.go(UploadNavKey(), ExportedNavKey(ID))

        shell.clickClose()

        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey)
        assertThat(rule.runOnIdle { shell.navigator.goBack() }).isFalse()
        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey)
    }

    @Test
    fun paywallCloseGoesBack() {
        shell.hasHome = true
        shell.show(DefaultApplicationsNavKey)
        shell.go(JobNavKey(), JobResultNavKey(ID), PaywallNavKey(ID))
        rule.onAllNodesWithText(name(PaywallNavKey(ID))).assertCountEquals(1)

        shell.clickClose()

        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey, JobNavKey(), JobResultNavKey(ID)).inOrder()
    }
}
