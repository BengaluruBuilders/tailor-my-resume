package com.tailormyresume.feature.tailor.impl.tailoring

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
class TailoringScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun shoot(percent: Int, device: TmrTestDevice, suffix: String) {
        val state = TailoringUiState(
            percent = percent,
            rows = tailoringRows(percent, addsExample = true),
            keywordCount = 5,
            stickers = listOf("SQL", "Power BI", "Stakeholders"),
        )
        rule.captureResultScreen("result_tailoring_$percent$suffix", device) { TailoringScreen(state) }
        rule.onNodeWithText("Tailoring your resume").assertExists()
        rule.onNodeWithText("Matching 5 keywords").assertExists()
        rule.onNodeWithText("Adding your example").assertExists()
        rule.onNodeWithText("Fitting to 1 page").assertExists()
    }

    @Test
    fun tailoring0() = shoot(0, TmrTestDevices.prototype, "")

    @Test
    fun tailoring50() = shoot(50, TmrTestDevices.prototype, "")

    @Test
    fun tailoring100() = shoot(100, TmrTestDevices.prototype, "")

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun tailoring50_font2_337dp() = shoot(50, NARROW_DEVICE, "")
}
