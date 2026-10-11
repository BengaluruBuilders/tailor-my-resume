package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrInputLargeFontScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonPrimaryAtFontScale200() {
        val device = TmrTestDevices.prototypeLargeFont
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, device.fontScale)) {
                TmrPreviewTheme {
                    Column(modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background).padding(24.dp)) {
                        TmrPrimaryButton(label = "Looks right, continue", onClick = {}, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        runBlocking {
            composeRule.captureForDevice(outputDirectory = "src/test/screenshots", screenName = "input_button_primary", device = device)
        }
    }
}
