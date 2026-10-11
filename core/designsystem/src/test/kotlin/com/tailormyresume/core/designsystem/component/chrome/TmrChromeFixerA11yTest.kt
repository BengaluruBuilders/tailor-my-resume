package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
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

private const val FIX_FRAME_MS = 16L
private const val FIX_TOAST_DISPLAY_MS = 3200L
private const val FIX_TOAST_ELAPSED_MS = 60_000L
private const val FIX_CUSTOM_TOAST_MS = 5000
private const val FIX_FONT_SCALE = 2f
private const val FIX_PARENT_TAG = "fixerParent"
private val FIX_PARENT_WIDTH = 337.dp

private fun ComposeTestRule.settleFixerToast(
    state: TmrToastState,
    message: String,
    action: TmrToastAction? = null,
    durationMillis: Int = FIX_TOAST_DISPLAY_MS.toInt(),
) {
    runOnIdle { state.show(message, action, durationMillis) }
    repeat(2) {
        mainClock.advanceTimeBy(FIX_FRAME_MS)
        waitForIdle()
    }
}

private fun ComposeTestRule.assertNoSplitWords(label: String, useUnmergedTree: Boolean = true) {
    val results = mutableListOf<TextLayoutResult>()
    onNode(
        hasText(label, ignoreCase = true) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
        useUnmergedTree = useUnmergedTree,
    ).fetchSemanticsNode()
        .config[SemanticsActions.GetTextLayoutResult]
        .action
        ?.invoke(results)
    val layout = requireNotNull(results.firstOrNull())
    assertFalse("$label overflows", layout.didOverflowHeight)
    val lines =
        List(layout.lineCount) { index ->
            layout.layoutInput.text.text.substring(layout.getLineStart(index), layout.getLineEnd(index)).trim()
        }
    for (index in lines.indices) {
        assertFalse("$label line $index is ellipsized", layout.isLineEllipsized(index))
    }
    assertTrue(label.equals(lines.joinToString(" "), ignoreCase = true))
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrChromeWholeWordsA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setLargeTextContent(
        fontScale: Float = FIX_FONT_SCALE,
        content: @Composable () -> Unit,
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                val base = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(base, fontScale)) {
                    Box(Modifier.width(FIX_PARENT_WIDTH).testTag(FIX_PARENT_TAG)) {
                        content()
                    }
                }
            }
        }
    }

    @Test
    fun longTopBarTitleWrapsAtOneXInsteadOfClipping() {
        val title = "Senior Backend Platform Engineering Manager"
        setLargeTextContent(fontScale = 1f) {
            TmrTopBar(leading = TmrTopBarLeading.Back, onLeading = {}, title = title)
        }
        val results = mutableListOf<TextLayoutResult>()
        composeRule
            .onNode(
                hasText(title, ignoreCase = true) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        val layout = requireNotNull(results.firstOrNull())
        assertFalse(layout.didOverflowWidth)
        assertFalse(layout.didOverflowHeight)
        assertTrue(layout.lineCount > 1)
    }

    @Test
    fun stepBarLabelsKeepWholeWordsAtFontScale2() {
        setLargeTextContent {
            TmrStepBar(current = 2, modifier = Modifier.fillMaxWidth())
        }
        for (label in listOf("✓ Profile", "2 Job", "3 Tailor")) {
            composeRule.assertNoSplitWords(label)
        }
    }

    @Test
    fun tabBarLabelsKeepWholeWordsAtFontScale2() {
        setLargeTextContent {
            TmrTabBar(
                selected = TmrTab.Applications,
                onApplications = {},
                onAdd = {},
                onProfile = {},
            )
        }
        for (label in listOf("Applications", "Profile")) {
            composeRule.assertNoSplitWords(label, useUnmergedTree = false)
        }
    }

    @Test
    fun topBarTitleKeepsWholeWordsAtFontScale2() {
        setLargeTextContent {
            TmrTopBar(
                leading = TmrTopBarLeading.Back,
                onLeading = {},
                title = "Resume",
                action = "Save",
                onAction = {},
            )
        }
        composeRule.assertNoSplitWords("Resume", useUnmergedTree = false)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrToastTimeoutA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setHost(
        state: TmrToastState,
        screenReader: Boolean,
        recommendedTimeoutMillis: (Int, Int) -> Int = { original, _ -> original },
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrToastHost(
                        state = state,
                        recommendedTimeoutMillis = recommendedTimeoutMillis,
                        isScreenReaderEnabled = { screenReader },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showWithAction(state: TmrToastState) {
        composeRule.settleFixerToast(
            state = state,
            message = "Applied, marked today",
            action = TmrToastAction(label = "Undo", onClick = {}),
        )
    }

    @Test
    fun dismissSemanticsActionClearsToastWithoutRunningAction() {
        val state = TmrToastState()
        var actionRuns = 0
        setHost(state, screenReader = true)
        composeRule.settleFixerToast(
            state = state,
            message = "Applied, marked today",
            action = TmrToastAction(label = "Undo", onClick = { actionRuns++ }),
        )
        composeRule.runOnIdle {
            val config = composeRule.onNodeWithTag(TmrChromeTags.TOAST).fetchSemanticsNode().config
            assertTrue(config[SemanticsActions.Dismiss].action?.invoke() == true)
        }
        composeRule.runOnIdle {
            assertNull(state.current)
            assertEquals(0, actionRuns)
        }
    }

    @Test
    fun actionToastWithScreenReaderWaitsForDismissal() {
        val state = TmrToastState()
        setHost(state, screenReader = true)
        showWithAction(state)
        composeRule.mainClock.advanceTimeBy(FIX_TOAST_ELAPSED_MS, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNotNull(state.current) }
        composeRule.runOnIdle { state.dismiss() }
        composeRule.runOnIdle { assertNull(state.current) }
    }

    @Test
    @Config(sdk = [28])
    fun actionToastWithScreenReaderWaitsForDismissalOnSdk28() {
        val state = TmrToastState()
        setHost(state, screenReader = true)
        showWithAction(state)
        composeRule.mainClock.advanceTimeBy(FIX_TOAST_ELAPSED_MS, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNotNull(state.current) }
    }

    @Test
    fun actionlessToastWithScreenReaderStillTimesOut() {
        val state = TmrToastState()
        setHost(state, screenReader = true)
        composeRule.settleFixerToast(state = state, message = "Changes saved")
        composeRule.mainClock.advanceTimeBy(FIX_TOAST_DISPLAY_MS, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }
    }

    @Test
    fun actionlessToastHonoursItsOwnDurationWithoutScreenReader() {
        val state = TmrToastState()
        setHost(state, screenReader = false)
        composeRule.settleFixerToast(
            state = state,
            message = "Changes saved",
            durationMillis = FIX_CUSTOM_TOAST_MS,
        )
        composeRule.mainClock.advanceTimeBy(FIX_CUSTOM_TOAST_MS - 1L, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNotNull(state.current) }
        composeRule.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }
    }

    @Test
    @Config(sdk = [28])
    fun actionToastOnSdk28NeverConsultsRecommendedTimeout() {
        val state = TmrToastState()
        setHost(
            state = state,
            screenReader = false,
            recommendedTimeoutMillis = { _, _ -> error("recommendedTimeoutMillis was consulted") },
        )
        showWithAction(state)
        composeRule.mainClock.advanceTimeBy(FIX_TOAST_DISPLAY_MS, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrSheetTitleA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sheetExposesItsTitleAsPaneTitle() {
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrBottomSheet(onDismiss = {}, title = "Change status") {
                        Text("Body")
                    }
                }
            }
        }
        val paneTitle =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.PaneTitle)
        assertEquals("Change status", paneTitle)
    }
}
