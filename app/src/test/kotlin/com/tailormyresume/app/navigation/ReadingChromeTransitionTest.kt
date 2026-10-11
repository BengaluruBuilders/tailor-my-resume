package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

private const val FRAMES_PER_STEP = 40

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingChromeTransitionTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val shell = ShellHarness(rule)

    private fun backShownWhileReadingIsOnScreen(): Boolean {
        val reading = rule.onAllNodes(hasText("ReadingNavKey")).fetchSemanticsNodes().isNotEmpty()
        val back = rule.onAllNodes(hasContentDescription("Back")).fetchSemanticsNodes().isNotEmpty()
        return reading && back
    }

    private fun framesWithBackOverReading(trigger: () -> Unit): Int {
        trigger()
        var offending = 0
        repeat(FRAMES_PER_STEP) {
            rule.mainClock.advanceTimeByFrame()
            if (backShownWhileReadingIsOnScreen()) offending += 1
        }
        return offending
    }

    @Test
    fun readingNeverShowsBackWhileEnteringStayingOrLeaving() {
        shell.show(UploadNavKey())
        shell.go(PasteResumeNavKey())
        rule.mainClock.autoAdvance = false

        val entering = framesWithBackOverReading { rule.runOnIdle { shell.navigator.replace(ReadingNavKey()) } }
        val leaving = framesWithBackOverReading { rule.runOnIdle { shell.navigator.replace(ReviewProfileNavKey()) } }

        assertThat(entering).isEqualTo(0)
        assertThat(leaving).isEqualTo(0)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertThat(rule.onAllNodes(hasContentDescription("Back")).fetchSemanticsNodes()).hasSize(1)
    }
}
