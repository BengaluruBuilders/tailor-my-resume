package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class UploadErrorScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun uploadError() {
        reduceMotion()
        composeRule.showFlowScreen {
            UnreadableScreen(failure = ImageOnlyFailure, onChooseAnotherClick = {}, onPasteClick = {})
        }
        composeRule.captureFlowScreen("uploadError")
    }

    @Test
    fun uploadErrorNeutral() {
        reduceMotion()
        composeRule.showFlowScreen {
            UnreadableScreen(failure = NeutralFailure, onChooseAnotherClick = {}, onPasteClick = {})
        }
        composeRule.captureFlowScreen("uploadError_neutral")
    }
}
