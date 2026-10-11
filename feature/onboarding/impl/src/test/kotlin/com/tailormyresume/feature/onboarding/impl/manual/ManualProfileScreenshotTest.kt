package com.tailormyresume.feature.onboarding.impl.manual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import com.tailormyresume.feature.onboarding.impl.upload.captureFlowScreen
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal val SampleManualState = ManualProfileUiState(fullName = "Priya Deshmukh", email = "priya.deshmukh@gmail.com")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ManualProfileScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun manual() {
        reduceMotion()
        composeRule.showFlowScreen {
            ManualProfileScreen(SampleManualState, onFieldChange = { _, _ -> }, onContinueClick = {})
        }
        composeRule.captureFlowScreen("manual")
    }
}
