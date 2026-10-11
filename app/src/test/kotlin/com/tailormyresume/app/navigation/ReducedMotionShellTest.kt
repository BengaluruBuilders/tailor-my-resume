package com.tailormyresume.app.navigation

import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

private const val MAX_FRAMES_TO_APPEAR = 10

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReducedMotionShellTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shell = ShellHarness(rule)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun restoreAnimations() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    private fun animationScale(scale: Float) {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    private fun firstFrameAndSettledBounds(
        target: SemanticsMatcher,
        trigger: () -> Unit,
    ): Pair<DpRect, DpRect> {
        trigger()
        var frames = 0
        do {
            rule.mainClock.advanceTimeByFrame()
            frames += 1
        } while (rule.onAllNodes(target).fetchSemanticsNodes().isEmpty() && frames < MAX_FRAMES_TO_APPEAR)
        val first = rule.onNode(target).getBoundsInRoot()
        rule.mainClock.advanceTimeBy(2_000)
        return first to rule.onNode(target).getBoundsInRoot()
    }

    @Test
    fun noTransitionWhenReduced() {
        animationScale(0f)
        rule.mainClock.autoAdvance = false
        shell.show(SignInNavKey())

        val (first, settled) = firstFrameAndSettledBounds(hasText("UploadNavKey")) {
            rule.runOnIdle { shell.navigator.navigate(UploadNavKey()) }
        }
        assertThat(first).isEqualTo(settled)

        val (tabFirst, tabSettled) = firstFrameAndSettledBounds(hasContentDescription("New application")) {
            rule.runOnIdle { shell.navigator.root(DefaultApplicationsNavKey) }
        }
        assertThat(tabFirst).isEqualTo(tabSettled)
    }

    @Test
    fun screensMoveWhenMotionIsNotReduced() {
        rule.mainClock.autoAdvance = false
        shell.show(SignInNavKey())

        val (first, settled) = firstFrameAndSettledBounds(hasText("UploadNavKey")) {
            rule.runOnIdle { shell.navigator.navigate(UploadNavKey()) }
        }

        assertThat(first).isNotEqualTo(settled)
    }
}
