package com.tailormyresume.feature.analysis.impl.job

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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import com.tailormyresume.feature.analysis.impl.joblink.JobLinkScreen
import com.tailormyresume.feature.analysis.impl.joblink.JobLinkUiState
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

private const val NORTHWIND_JOB_POST =
    "Associate Analyst, Northwind GCC\nBengaluru · Hybrid · Full-time\n\nAbout the role\nYou'll join the Finance Analytics team supporting business partners across EMEA. " +
        "You'll build and maintain Power BI dashboards, write SQL against large datasets, and turn findings into clear " +
        "recommendations for senior stakeholders.\n\nWhat you'll need\n• 2+ years in analytics or finance\n• Strong SQL and Excel\n" +
        "• Power BI or Tableau\n• Financial reporting and variance analysis\n• Experience presenting to senior stakeholders\n\n" +
        "Nice to have\n• Python\n• Forecasting models"

private const val REFERRAL_MESSAGE =
    "Hey Priya, we spoke last week about the analyst role at Northwind. My cousin is on that team and asked me to pass " +
        "your name along, so do shout if you want me to send her your resume."

private const val LINK = "https://careers.northwind.example/associate-analyst"

private sealed interface Screen {
    data class Job(val state: JobUiState) : Screen

    data object Link : Screen
}

private val SCREENS: List<Pair<String, Screen>> = listOf(
    "job_empty" to Screen.Job(JobUiState.Empty),
    "job_hasText_detected" to Screen.Job(
        JobUiState.HasText(text = NORTHWIND_JOB_POST, detected = "Associate Analyst · Northwind GCC"),
    ),
    "job_notAJobPost" to Screen.Job(JobUiState.HasText(text = REFERRAL_MESSAGE, notAJobPost = true)),
    "job_analyzing_37percent" to Screen.Job(JobUiState.Analyzing(text = NORTHWIND_JOB_POST, percent = 37)),
    "joblink_default" to Screen.Link,
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JobScreenshotTest {

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

    private fun captureStates(device: TmrTestDevice) {
        var screen by mutableStateOf(SCREENS.first().second)
        rule.setContent {
            TmrTheme {
                Box(modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, device.fontScale),
                    ) {
                        when (val current = screen) {
                            is Screen.Job -> JobScreen(
                                state = current.state,
                                onTextChange = {},
                                onPaste = {},
                                onUseLink = {},
                                onClear = {},
                                onAnalyze = {},
                            )

                            Screen.Link -> JobLinkScreen(
                                state = JobLinkUiState(link = LINK),
                                onLinkChange = {},
                                onImport = {},
                            )
                        }
                    }
                }
            }
        }
        SCREENS.forEach { (screenName, next) ->
            screen = next
            rule.waitForIdle()
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

    private fun textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> =
        rule.onAllNodes(SemanticsMatcher("has text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) })
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.firstOrNull()?.let { node to it }
            }
}
