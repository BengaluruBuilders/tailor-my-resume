package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.feature.onboarding.impl.manual.ManualProfileScreen
import com.tailormyresume.feature.onboarding.impl.manual.SampleManualState
import com.tailormyresume.feature.onboarding.impl.paste.PasteResumeScreen
import com.tailormyresume.feature.onboarding.impl.paste.PasteResumeUiState
import com.tailormyresume.feature.onboarding.impl.paste.SAMPLE_RESUME_TEXT
import com.tailormyresume.feature.onboarding.impl.reading.ReadingScreen
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_DEVICE
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = NARROW_QUALIFIERS)
class UploadNarrowScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showFlowScreen(NARROW_DEVICE.fontScale, content)
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.captureFlowScreen(name, NARROW_DEVICE)
    }

    @Test
    fun upload() = capture("upload") { UploadScreen(onUploadClick = {}, onPasteClick = {}, onManualClick = {}) }

    @Test
    fun uploadError() = capture("uploadError") {
        UnreadableScreen(failure = ImageOnlyFailure, onChooseAnotherClick = {}, onPasteClick = {})
    }

    @Test
    fun uploadErrorNeutral() = capture("uploadError_neutral") {
        UnreadableScreen(failure = NeutralFailure, onChooseAnotherClick = {}, onPasteClick = {})
    }

    @Test
    fun uploadErrorUnseparatedFileName() = capture("uploadError_longName") {
        UnreadableScreen(failure = UnseparatedNameFailure, onChooseAnotherClick = {}, onPasteClick = {})
    }

    @Test
    fun pasteResume() = capture("pasteResume") {
        PasteResumeScreen(PasteResumeUiState(SAMPLE_RESUME_TEXT, canRead = true), onTextChange = {}, onReadClick = {})
    }

    @Test
    fun manual() = capture("manual") {
        ManualProfileScreen(SampleManualState, onFieldChange = { _, _ -> }, onContinueClick = {})
    }

    @Test
    fun reading_0() = capture("reading_0") { ReadingScreen(readingStateAt(0)) }

    @Test
    fun reading_40() = capture("reading_40") { ReadingScreen(readingStateAt(40)) }

    @Test
    fun reading_100() = capture("reading_100") { ReadingScreen(readingStateAt(100)) }
}
