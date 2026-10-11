package com.tailormyresume.feature.onboarding.impl.reading

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import com.tailormyresume.feature.onboarding.impl.upload.captureFlowScreen
import com.tailormyresume.feature.onboarding.impl.upload.readingStateAt
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ReadingScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun captureAt(percent: Int) {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showFlowScreen { ReadingScreen(readingStateAt(percent)) }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.captureFlowScreen("reading_$percent")
    }

    @Test
    fun reading_0() = captureAt(0)

    @Test
    fun reading_40() = captureAt(40)

    @Test
    fun reading_100() = captureAt(100)
}
