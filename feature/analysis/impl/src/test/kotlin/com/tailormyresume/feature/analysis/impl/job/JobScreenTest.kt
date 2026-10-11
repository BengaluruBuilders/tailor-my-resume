package com.tailormyresume.feature.analysis.impl.job

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val FAILED_MESSAGE = "Couldn't read the job. Try again."
private const val NOT_AVAILABLE = "Not available yet"
private const val ANALYZING_37 = "Reading the job… 37%"
private const val NARROW_CONTAINER_DP = 337
private const val FONT_SCALE_200 = 2.0f
private const val SCREEN_READER_PERSIST_MS = 60_000L
private const val MAX_FRAMES = 120
private const val TOAST_SETTLE_MS = 1_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class JobScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private class Calls {
        var textChanges = 0
        var pastes = 0
        var links = 0
        var clears = 0
        var analyzes = 0
    }

    @After
    fun restoreAnimationScale() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    @Test
    fun emptyScreenShowsHeroPasteAndLinkButtonsAndDisabledAnalyze() {
        setScreen(JobUiState.Empty)
        rule.onNodeWithText("Paige reads it for you", ignoreCase = true).assertIsDisplayed()
        rule.onNodeWithText("Paste the job you want").assertIsDisplayed()
        rule.onNodeWithText("Paste from clipboard").assertIsDisplayed()
        rule.onNodeWithText("Use a link").assertIsDisplayed()
        rule
            .onNodeWithText("Include requirements and responsibilities for the most accurate analysis")
            .assertIsDisplayed()
        val config = rule.onNodeWithText("Analyze job").fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.Disabled)).isNotEqualTo(false)
        assertThat(config.getOrNull(SemanticsProperties.StateDescription)).isEqualTo(NOT_AVAILABLE)
    }

    @Test
    fun pasteAndLinkButtonsInvokeCallbacks() {
        val calls = Calls()
        setScreen(JobUiState.Empty, calls)
        rule.onNodeWithText("Paste from clipboard").performClick()
        rule.onNodeWithText("Use a link").performClick()
        rule.runOnIdle {
            assertThat(calls.pastes).isEqualTo(1)
            assertThat(calls.links).isEqualTo(1)
        }
    }

    @Test
    fun hasTextShowsHeaderClearCountAndDetected() {
        val calls = Calls()
        setScreen(JobUiState.HasText(text = jobText(1234), detected = DETECTED), calls)
        rule.onNodeWithText("Job description").assertIsDisplayed()
        rule.onNodeWithText("Clear").assertIsDisplayed()
        rule.onNodeWithText("1,234 characters").assertIsDisplayed()
        rule.onNodeWithText(DETECTED).assertIsDisplayed()
        val analyze = rule.onNodeWithText("Analyze job").fetchSemanticsNode().config
        assertThat(analyze.getOrNull(SemanticsProperties.StateDescription)).isNull()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithText("Analyze job").performClick()
        rule.runOnIdle {
            assertThat(calls.clears).isEqualTo(1)
            assertThat(calls.analyzes).isEqualTo(1)
        }
    }

    @Test
    fun notAJobPostShowsAmberCard() {
        setScreen(JobUiState.HasText(text = jobText(40), notAJobPost = true))
        rule.onNodeWithText("This doesn't look like a job post").assertIsDisplayed()
        rule
            .onNodeWithText("Paste the full listing, including responsibilities and requirements.")
            .assertIsDisplayed()
    }

    @Test
    fun blankNotAJobPostShowsPasteActionsAndDisabledAnalyze() {
        val calls = Calls()
        setScreen(JobUiState.HasText(text = "", notAJobPost = true), calls)
        rule.onNodeWithText("This doesn't look like a job post").assertIsDisplayed()
        rule.onNodeWithText("Paste from clipboard").assertIsDisplayed()
        rule.onNodeWithText("Use a link").assertIsDisplayed()
        val config = rule.onNodeWithText("Analyze job").fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.StateDescription)).isEqualTo(NOT_AVAILABLE)
        rule.onNodeWithText("Paste from clipboard").performClick()
        rule.onNodeWithText("Use a link").performClick()
        rule.runOnIdle {
            assertThat(calls.pastes).isEqualTo(1)
            assertThat(calls.links).isEqualTo(1)
        }
    }

    @Test
    fun whitespaceOnlyTextDisablesAnalyze() {
        setScreen(JobUiState.HasText(text = "   \n ", notAJobPost = true))
        rule.onNodeWithText("Paste from clipboard").assertIsDisplayed()
        val config = rule.onNodeWithText("Analyze job").fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.StateDescription)).isEqualTo(NOT_AVAILABLE)
    }

    @Test
    fun analyzingShowsPercentPill() {
        setScreen(JobUiState.Analyzing(text = jobText(200), percent = 37))
        rule.onNodeWithText(ANALYZING_37).assertIsDisplayed()
        rule.onNodeWithText("Analyze job").assertDoesNotExist()
    }

    @Test
    fun reducedMotionShowsPercentWithoutAnimation() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        rule.mainClock.autoAdvance = false
        setScreen(JobUiState.Analyzing(text = jobText(200), percent = 37))
        rule.onNodeWithText(ANALYZING_37).assertIsDisplayed()
    }

    @Test
    fun retryToastActionInvokesRetry() {
        val toastState = TmrToastState()
        val events = Channel<JobEvent>(Channel.UNLIMITED)
        var retries = 0
        setHandler(toastState, events, onRetry = { retries += 1 })
        rule.runOnIdle { events.trySend(JobEvent.AnalysisFailed) }
        advanceUntil { toastState.current != null }
        rule.onNodeWithText(FAILED_MESSAGE).assertIsDisplayed()
        rule.onNodeWithText("Retry").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        rule.runOnIdle { assertThat(retries).isEqualTo(1) }
    }

    @Test
    fun retryToastStaysWithScreenReader() {
        val toastState = TmrToastState()
        val events = Channel<JobEvent>(Channel.UNLIMITED)
        setHandler(toastState, events, screenReader = true)
        rule.runOnIdle { events.trySend(JobEvent.AnalysisFailed) }
        advanceUntil { toastState.current != null }
        rule.mainClock.advanceTimeBy(SCREEN_READER_PERSIST_MS, ignoreFrameDuration = true)
        rule.waitForIdle()
        rule.onNodeWithText(FAILED_MESSAGE).assertIsDisplayed()
        rule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun importedToastShowsHost() {
        val toastState = TmrToastState()
        val events = Channel<JobEvent>(Channel.UNLIMITED)
        setHandler(toastState, events)
        rule.runOnIdle { events.trySend(JobEvent.Imported("careers.northwind.example")) }
        advanceUntil { toastState.current != null }
        rule.onNodeWithText("Imported from careers.northwind.example").assertIsDisplayed()
    }

    @Test
    fun analyzedEventCallsOnAnalyzedWithId() {
        val toastState = TmrToastState()
        val events = Channel<JobEvent>(Channel.UNLIMITED)
        val ids = mutableListOf<String>()
        setHandler(toastState, events, onAnalyzed = { ids += it })
        rule.runOnIdle { events.trySend(JobEvent.Analyzed("app-7")) }
        advanceUntil { ids.isNotEmpty() }
        rule.runOnIdle { assertThat(ids).containsExactly("app-7") }
    }

    @Test
    fun noLabelEllipsizedAtFont200() {
        val state = mutableStateOf<JobUiState>(JobUiState.Empty)
        val calls = Calls()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, FONT_SCALE_200),
            ) {
                TmrTheme {
                    Box(Modifier.width(NARROW_CONTAINER_DP.dp)) {
                        JobScreen(
                            state = state.value,
                            onTextChange = { calls.textChanges += 1 },
                            onPaste = { calls.pastes += 1 },
                            onUseLink = { calls.links += 1 },
                            onClear = { calls.clears += 1 },
                            onAnalyze = { calls.analyzes += 1 },
                        )
                    }
                }
            }
        }
        assertLabelsFit("Paste from clipboard", "Use a link", "Analyze job")
        rule.runOnIdle { state.value = JobUiState.HasText(text = jobText(1234), detected = DETECTED) }
        assertLabelsFit("Analyze job", "Clear")
    }

    private fun setScreen(state: JobUiState, calls: Calls = Calls()) {
        rule.setContent {
            TmrTheme {
                Box(Modifier.fillMaxSize()) {
                    JobScreen(
                        state = state,
                        onTextChange = { calls.textChanges += 1 },
                        onPaste = { calls.pastes += 1 },
                        onUseLink = { calls.links += 1 },
                        onClear = { calls.clears += 1 },
                        onAnalyze = { calls.analyzes += 1 },
                    )
                }
            }
        }
    }

    private fun setHandler(
        toastState: TmrToastState,
        events: Channel<JobEvent>,
        screenReader: Boolean = false,
        onAnalyzed: (String) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalTmrToast provides toastState) {
                    Box(Modifier.fillMaxSize()) {
                        JobEventHandler(
                            events = events.receiveAsFlow(),
                            onAnalyzed = onAnalyzed,
                            onRetry = onRetry,
                        )
                        TmrToastHost(toastState, isScreenReaderEnabled = { screenReader })
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun advanceUntil(predicate: () -> Boolean) {
        var frames = 0
        while (!predicate() && frames < MAX_FRAMES) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            frames += 1
        }
        rule.mainClock.advanceTimeBy(TOAST_SETTLE_MS, ignoreFrameDuration = true)
        rule.waitForIdle()
    }

    private fun assertLabelsFit(vararg labels: String) {
        labels.forEach { label ->
            val node = rule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val layout = layouts.first()
            assertThat(layout.didOverflowHeight).isFalse()
            assertThat((0 until layout.lineCount).any { layout.isLineEllipsized(it) }).isFalse()
            val text = layout.layoutInput.text.text
            assertThat(text).doesNotContain("…")
            repeat(layout.lineCount - 1) { line ->
                val lineEnd = layout.getLineEnd(line, visibleEnd = false)
                assertThat(text.substring(0, lineEnd)).endsWith(" ")
            }
        }
    }

    private fun jobText(length: Int): String = "a".repeat(length)

    private companion object {
        const val DETECTED = "Associate Analyst · Northwind GCC"
    }
}
