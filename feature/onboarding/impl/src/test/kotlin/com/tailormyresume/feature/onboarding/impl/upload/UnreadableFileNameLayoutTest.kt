package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.TmrWholeWordTextLayoutKey
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_DEVICE
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = NARROW_QUALIFIERS)
class UnreadableFileNameLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nameWithoutSeparatorsNeverBreaksInsideAWord() {
        composeRule.showFlowScreen(NARROW_DEVICE.fontScale) {
            UnreadableScreen(failure = UnseparatedNameFailure, onChooseAnotherClick = {}, onPasteClick = {})
        }

        val layout = checkNotNull(
            composeRule.onNodeWithText("PriyaDeshmukhResume2026.pdf").fetchSemanticsNode().config[TmrWholeWordTextLayoutKey].invoke(),
        )
        val lines = (0 until layout.lineCount).map { line ->
            layout.layoutInput.text.text.substring(layout.getLineStart(line), layout.getLineEnd(line, visibleEnd = false))
        }
        lines.dropLast(1).forEach { line -> assertThat(line.trimEnd('\u200B')).endsWith(".") }
    }
}
