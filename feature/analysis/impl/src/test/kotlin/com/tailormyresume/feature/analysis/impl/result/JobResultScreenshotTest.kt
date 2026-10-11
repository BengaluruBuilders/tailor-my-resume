package com.tailormyresume.feature.analysis.impl.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val NARROW_QUALIFIERS = "w337dp-h734dp-normal-long-notround-any-480dpi-keyshidden-nonav"

private val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

private val CREDITS_STATE = JobResultUiState.Ready(
    title = "Associate Analyst",
    company = "Northwind GCC",
    location = "Bengaluru · Hybrid",
    now = 61,
    upTo = 92,
    have = listOf("SQL", "Excel", "Power BI", "Reporting", "Variance analysis"),
    missing = listOf("Tableau", "Forecasting", "Stakeholder management"),
    mustHaves = listOf(
        MustHaveRow("r1", "2+ years in analytics", MatchStatus.MET, "You have 4 years", false),
        MustHaveRow("r2", "Strong SQL and Excel", MatchStatus.MET, "Used at Infosys and Tata Digital", false),
        MustHaveRow("r3", "Presenting to senior stakeholders", MatchStatus.GAP, null, true),
        MustHaveRow(
            "r4",
            "Financial reporting and variance analysis, including month-end close commentary",
            MatchStatus.GAP,
            null,
            false,
        ),
    ),
    credits = 3,
    asksQuestion = true,
)

private const val LONG_WORD = "Internationalization"

private val LONG_WORD_STATE = CREDITS_STATE.copy(
    title = "$LONG_WORD Analyst",
    company = LONG_WORD,
    mustHaves = listOf(
        MustHaveRow("r1", "$LONG_WORD of reporting", MatchStatus.MET, "Led $LONG_WORD at Infosys", false),
        MustHaveRow("r2", LONG_WORD, MatchStatus.GAP, null, true),
    ),
)

private val NO_CREDITS_STATE = CREDITS_STATE.copy(credits = 0)

private val SCREENS: List<Pair<String, JobResultUiState.Ready>> = listOf(
    "result_job_analyzed_credits" to CREDITS_STATE,
    "result_job_analyzed_nocredits" to NO_CREDITS_STATE,
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JobResultScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

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
        setContent(CREDITS_STATE, NARROW_DEVICE)
        assertNoTruncatedText()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun longWordNeverBreaksInsideAWordAtFont200At337dp() {
        setContent(LONG_WORD_STATE, NARROW_DEVICE)
        assertLongWordsStayWhole()
        assertNoTruncatedText()
    }

    private fun captureStates(device: TmrTestDevice) {
        var screen by mutableStateOf(SCREENS.first().second)
        rule.setContent { ResultContent(screen, device) }
        SCREENS.forEach { (screenName, next) ->
            screen = next
            rule.waitForIdle()
            assertResultContent(next)
            runBlocking {
                rule.captureForDevice(
                    outputDirectory = "src/test/screenshots",
                    screenName = screenName,
                    device = device,
                )
            }
            assertNoTruncatedText()
        }
    }

    private fun setContent(state: JobResultUiState.Ready, device: TmrTestDevice) {
        rule.setContent { ResultContent(state, device) }
        rule.waitForIdle()
        assertResultContent(state)
    }

    @Composable
    private fun ResultContent(state: JobResultUiState.Ready, device: TmrTestDevice) {
        TmrTheme {
            Box(modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, device.fontScale),
                ) {
                    JobResultScreen(state = state, onTailor = {})
                }
            }
        }
    }

    private fun assertResultContent(state: JobResultUiState.Ready) {
        rule.onNodeWithText("Tailor my resume", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Keywords matched", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("What they screen for · 8", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Must-haves", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Not clear in your resume. We'll ask you next.", useUnmergedTree = true).assertExists()
        if (state.credits > 0) {
            rule.onNodeWithText("Uses 1 credit · you have ${state.credits}", useUnmergedTree = true).assertExists()
        } else {
            rule.onNodeWithText("No credits left · get more to continue", useUnmergedTree = true).assertExists()
        }
        rule.onNodeWithText(state.title, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("${state.company} · ${state.location}", useUnmergedTree = true).assertExists()
    }

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
