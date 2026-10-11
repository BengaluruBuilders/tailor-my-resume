package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabBarVisibilityTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shell = ShellHarness(rule)

    @Test
    fun tabsOnlyOnApplicationsAndProfile() {
        shell.show(SignInNavKey())

        CHROME_TABLE.forEach { row ->
            shell.root(row.key)
            val tabsExpected = row.key is ApplicationsNavKey || row.key is ProfileNavKey
            rule.onAllNodes(hasContentDescription("New application"))
                .assertCountEquals(if (tabsExpected) 1 else 0)
        }
    }

    @Test
    fun tabSwitchUsesRootAndMarksTheCurrentTab() {
        shell.show(DefaultApplicationsNavKey)
        rule.onNode(isSelectable() and hasText("Applications", ignoreCase = true)).assertIsSelected()

        rule.onNode(isSelectable() and hasText("Profile", ignoreCase = true)).performClick()
        rule.waitForIdle()

        assertThat(shell.stack).containsExactly(DefaultProfileNavKey)
        rule.onNode(isSelectable() and hasText("Profile", ignoreCase = true)).assertIsSelected()

        rule.onNode(isSelectable() and hasText("Applications", ignoreCase = true)).performClick()
        rule.waitForIdle()

        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey)
    }
}
