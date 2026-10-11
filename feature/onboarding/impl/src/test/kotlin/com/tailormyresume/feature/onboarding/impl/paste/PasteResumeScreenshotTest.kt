package com.tailormyresume.feature.onboarding.impl.paste

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

internal const val SAMPLE_RESUME_TEXT =
    "Priya Deshmukh\nBusiness Analyst, Pune\npriya.deshmukh@gmail.com\n\nExperience\nBusiness Analyst, Northwind Traders, 2021 to Now\n" +
        "Built weekly sales dashboards used by four regional teams."

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class PasteResumeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pasteResume() {
        reduceMotion()
        composeRule.showFlowScreen {
            PasteResumeScreen(PasteResumeUiState(SAMPLE_RESUME_TEXT, canRead = true), onTextChange = {}, onReadClick = {})
        }
        composeRule.captureFlowScreen("pasteResume")
    }

    @Test
    fun pasteResumeEmpty() {
        reduceMotion()
        composeRule.showFlowScreen {
            PasteResumeScreen(PasteResumeUiState(), onTextChange = {}, onReadClick = {})
        }
        composeRule.captureFlowScreen("pasteResume_empty")
    }
}
