package com.tailormyresume.feature.onboarding.impl.review

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.content.TmrFitTextLayoutKey
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_DEVICE
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = NARROW_QUALIFIERS)
class ReviewLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun layoutOf(text: String): TextLayoutResult {
        val config = composeRule.onNodeWithText(text).fetchSemanticsNode().config
        config.getOrNull(TmrFitTextLayoutKey)?.invoke()?.let { return it }
        val layouts = mutableListOf<TextLayoutResult>()
        checkNotNull(config.getOrNull(SemanticsActions.GetTextLayoutResult)).action?.invoke(layouts)
        return layouts.first()
    }

    private fun assertLinesEndAtWordBoundaries(text: String) {
        val layout = layoutOf(text)
        val source = layout.layoutInput.text.text
        (0 until layout.lineCount - 1).forEach { line ->
            val end = layout.getLineEnd(line, visibleEnd = false)
            val last = source[end - 1]
            val boundary = last.isWhitespace() || last == '​' || last == '-' || source[end].isWhitespace()
            assertThat(boundary).isTrue()
        }
    }

    @Test
    fun noLabelBreaksInsideAWordAtFontScale2() {
        val profile = freshReviewProfile(sourceFileName = "Priya_Deshmukh_Resume_Final_2026.pdf")
        composeRule.showFlowScreen(NARROW_DEVICE.fontScale, noopReview(profile.reviewState()))

        assertLinesEndAtWordBoundaries("End date missing for Data Analyst Intern")
        assertLinesEndAtWordBoundaries("Add date")
        assertLinesEndAtWordBoundaries("From Priya_Deshmukh_Resume_Final_2026.pdf. Tap anything to edit.")
        assertLinesEndAtWordBoundaries("Looks right, continue")
    }
}
