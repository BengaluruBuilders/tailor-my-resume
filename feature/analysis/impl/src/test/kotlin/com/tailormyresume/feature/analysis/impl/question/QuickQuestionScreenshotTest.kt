package com.tailormyresume.feature.analysis.impl.question

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val NARROW_QUALIFIERS = "w337dp-h734dp-normal-long-notround-any-480dpi-keyshidden-nonav"

private val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

private const val QUESTION = "Have you presented to senior leaders?"

private const val WHY = "It's a must-have for this role, and your resume doesn't mention it."

private const val DETAIL =
    "Presented the monthly variance report to the CFO and the finance leadership team every quarter"

private const val HEADING = "One quick question"

private const val LONG_WORD = "Internationalization"

private val NONE_PICKED = QuickQuestionUiState.Ready(
    question = QUESTION,
    why = WHY,
    picked = null,
    detail = "",
)

private val LONG_WORD_STATE = NONE_PICKED.copy(why = "A must-have: $LONG_WORD experience is not on your resume.")

private val PICKED_WITH_DETAIL = QuickQuestionUiState.Ready(
    question = QUESTION,
    why = WHY,
    picked = QuickChoice.YES_REGULARLY,
    detail = DETAIL,
)

private val SCREENS: List<Pair<String, QuickQuestionUiState.Ready>> = listOf(
    "result_question_none" to NONE_PICKED,
    "result_question_picked_detail" to PICKED_WITH_DETAIL,
)

private val LABELS = listOf(
    HEADING,
    QUESTION,
    WHY,
    "Yes, regularly",
    "A few times",
    "Not yet",
    "Continue",
    "Skip this",
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickQuestionScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private var shown by mutableStateOf(NONE_PICKED)

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun statesAt1x() {
        captureStates(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun statesAtFont200At337dp() {
        captureStates(NARROW_DEVICE)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun noClippedLabelsAtFont200() {
        show(NARROW_DEVICE)
        SCREENS.forEach { (_, state) ->
            setScreen(state)
            assertLabelsExist()
            assertNoTruncatedText()
        }
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun longWordNeverBreaksInsideAWordAtFont200At337dp() {
        show(NARROW_DEVICE)
        setScreen(LONG_WORD_STATE)
        assertLongWordsStayWhole()
        assertNoTruncatedText()
    }

    private fun captureStates(device: TmrTestDevice) {
        show(device)
        SCREENS.forEach { (screenName, next) ->
            setScreen(next)
            runBlocking {
                rule.captureForDevice(
                    outputDirectory = "src/test/screenshots",
                    screenName = screenName,
                    device = device,
                )
            }
            assertLabelsExist()
            when (next) {
                NONE_PICKED -> {
                    assertEquals(0, textInputNodeCount())
                    assertNotNull("Continue must report it is not available", continueStateDescription())
                }

                else -> {
                    assertEquals(1, textInputNodeCount())
                    assertNull(continueStateDescription())
                }
            }
            assertNoTruncatedText()
        }
    }

    private fun show(device: TmrTestDevice) {
        rule.setContent {
            TmrTheme {
                Box(modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, device.fontScale)) {
                        QuickQuestionScreen(
                            state = shown,
                            onPick = {},
                            onDetailChange = {},
                            onContinue = {},
                            onSkip = {},
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun setScreen(state: QuickQuestionUiState.Ready) {
        shown = state
        rule.waitForIdle()
    }

    private fun assertLabelsExist() {
        LABELS.forEach { label ->
            rule.onNodeWithText(label).assertExists("missing label: $label")
        }
    }

    private fun textInputNodeCount(): Int =
        rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size

    private fun continueStateDescription(): String? =
        rule.onNodeWithText("Continue").fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun assertNoTruncatedText() {
        textLayouts().forEach { (node, layout) ->
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
            assertFalse("$text overflows its height", layout.didOverflowHeight)
            val scrollsItself = node.config.contains(SemanticsProperties.EditableText)
            assertTrue(
                "$text is clipped by its node",
                scrollsItself || (layout.size.width <= node.size.width && layout.size.height <= node.size.height),
            )
            val ellipsized = (0 until layout.lineCount).filter { layout.isLineEllipsized(it) }
            assertTrue("$text has ellipsized lines $ellipsized", ellipsized.isEmpty())
        }
    }

    private fun assertLongWordsStayWhole() {
        val carriers = textLayouts().filter { (_, layout) -> LONG_WORD in layout.layoutInput.text.text }
        assertTrue("no text carries the long word", carriers.isNotEmpty())
        carriers.forEach { (_, layout) ->
            val text = layout.layoutInput.text.text
            (0 until layout.lineCount - 1).forEach { line ->
                val end = layout.getLineEnd(line, visibleEnd = false)
                assertFalse(
                    "'$text' breaks inside a word at line $line",
                    end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace(),
                )
            }
        }
    }

    private fun textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> =
        rule.onAllNodes(SemanticsMatcher("has text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) })
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.firstOrNull()?.let { node to it }
            }
}
