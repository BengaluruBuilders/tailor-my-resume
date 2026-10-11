package com.tailormyresume.feature.applications.impl

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w337dp-h1600dp-normal-long-notround-any-480dpi-keyshidden-nonav")
class ApplicationsHeaderLayoutTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun showEmptyHome() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                TmrTheme {
                    ApplicationsScreen(
                        state = ApplicationsUiState.Empty(ApplicationsHeader(credits = 43, initials = "PD")),
                        onCredits = {},
                        onAvatar = {},
                        onAddJob = {},
                        onOpenApplication = {},
                    )
                }
            }
        }
    }

    private fun bounds(text: String) = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    @Test
    fun headerStartsBelowTheStatusBar() {
        val statusBarHeightPx = 96
        showEmptyHome()
        rule.runOnUiThread {
            val root = rule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusBarHeightPx, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(root as View, insets)
        }
        rule.waitForIdle()

        val titleTop = bounds("Applications").top
        val pillTop = bounds("43 credits").top
        val avatarTop = rule.onNodeWithContentDescription("Profile").fetchSemanticsNode().boundsInRoot.top
        assertTrue("title top $titleTop", titleTop >= statusBarHeightPx)
        assertTrue("pill top $pillTop", pillTop >= statusBarHeightPx)
        assertTrue("avatar top $avatarTop", avatarTop >= statusBarHeightPx)
    }

    @Test
    fun creditPillAndAvatarShareTheTitleRowAt337dp() {
        showEmptyHome()
        rule.waitForIdle()

        val title = bounds("Applications")
        val pill = bounds("43 credits")
        val avatar = rule.onNodeWithContentDescription("Profile").fetchSemanticsNode().boundsInRoot
        assertTrue("pill $pill below title $title", pill.top < title.bottom)
        assertTrue("avatar $avatar below title $title", avatar.top < title.bottom)
    }
}
