package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.tailor.impl.NARROW_DEVICE
import com.tailormyresume.feature.tailor.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.tailor.impl.captureResultScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TailorFailedScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun shoot(device: TmrTestDevice, screenName: String) {
        rule.captureResultScreen(screenName, device) { TailorFailedScreen(onTryAgain = {}, onGoBack = {}) }
        rule.onNodeWithText("Tailoring didn't finish").assertExists()
        rule.onNodeWithText("Something went wrong on our side. Your credit wasn't used.").assertExists()
        rule.onNodeWithText("Try again").assertExists()
        rule.onNodeWithText("Go back").assertExists()
    }

    @Test
    fun failed() = shoot(TmrTestDevices.prototype, "result_failed")

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun failed_font2_337dp() = shoot(NARROW_DEVICE, "result_failed")
}
