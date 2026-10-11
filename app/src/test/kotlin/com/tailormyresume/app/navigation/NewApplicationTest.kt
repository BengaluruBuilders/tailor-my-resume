package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NewApplicationTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shell = ShellHarness(rule)

    @Test
    fun plusPushesJobOnTopOfApplicationsAndProfile() {
        shell.show(DefaultApplicationsNavKey)

        shell.click("New application")
        assertThat(shell.stack).containsExactly(DefaultApplicationsNavKey, JobNavKey()).inOrder()

        shell.root(DefaultProfileNavKey)
        shell.click("New application")
        assertThat(shell.stack).containsExactly(DefaultProfileNavKey, JobNavKey()).inOrder()
    }
}
