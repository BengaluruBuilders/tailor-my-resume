package com.tailormyresume.core.designsystem.component.chrome

import android.view.accessibility.AccessibilityManager
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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.LocalTmrMotion
import com.tailormyresume.core.designsystem.theme.TmrMotion
import com.tailormyresume.core.designsystem.theme.TmrMotionDefaults
import com.tailormyresume.core.designsystem.theme.TmrSpacing
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

private const val A11Y_FRAME_MS = 16L
private const val A11Y_TOAST_DISPLAY_MS = 3200L
private const val A11Y_LONG_TIMEOUT_MS = 10_000
private const val A11Y_FONT_SCALE = 2f
private const val A11Y_STRESS_FONT_SCALE = 3f
private const val A11Y_PARENT_TAG = "parent"
private const val A11Y_STEP_COUNT = 3
private val A11Y_TOAST_TOP = 96.dp + 8.dp
private val A11Y_PARENT_WIDTH = 337.dp

private fun ComposeTestRule.textLayoutOf(text: String): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    val node = onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.first()
}

private fun ComposeTestRule.settleToast(
    state: TmrToastState,
    message: String,
    action: TmrToastAction? = null,
) {
    runOnIdle { state.show(message, action) }
    repeat(2) {
        mainClock.advanceTimeBy(A11Y_FRAME_MS)
        waitForIdle()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrChromeLargeTextA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setLargeTextContent(
        fontScale: Float = A11Y_FONT_SCALE,
        content: @Composable () -> Unit,
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                val base = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(base, fontScale)) {
                    Box(Modifier.width(A11Y_PARENT_WIDTH).testTag(A11Y_PARENT_TAG)) {
                        content()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun stepBarStaysInsideParentAtFontScale2() {
        setLargeTextContent {
            TmrStepBar(current = 2, modifier = Modifier.fillMaxWidth())
        }
        val parent = composeRule.onNodeWithTag(A11Y_PARENT_TAG).getBoundsInRoot()
        val pills =
            composeRule.onAllNodes(
                hasText("✓ Profile") or hasText("2 Job") or hasText("3 Tailor"),
                useUnmergedTree = true,
            )
        pills.assertCountEquals(A11Y_STEP_COUNT)
        for (index in 0 until A11Y_STEP_COUNT) {
            val bounds = pills[index].getBoundsInRoot()
            assertTrue(bounds.left >= parent.left)
            assertTrue(bounds.right <= parent.right)
            assertTrue(bounds.right <= A11Y_PARENT_WIDTH)
        }
    }

    @Test
    fun topBarStaysInsideParentAtFontScale2() {
        setLargeTextContent {
            TmrTopBar(
                leading = TmrTopBarLeading.Back,
                onLeading = {},
                title = "Resume",
                action = "Save",
                onAction = {},
            )
        }
        val parent = composeRule.onNodeWithTag(A11Y_PARENT_TAG).getBoundsInRoot()
        val actionBounds =
            composeRule
                .onNodeWithTag(TmrChromeTags.TOP_BAR_ACTION, useUnmergedTree = true)
                .assertTouchHeightIsEqualTo(48.dp)
                .getBoundsInRoot()
        val saveBounds = composeRule.onNodeWithText("Save", useUnmergedTree = true).getBoundsInRoot()
        assertTrue(actionBounds.right <= parent.right)
        assertTrue(actionBounds.right <= A11Y_PARENT_WIDTH)
        assertTrue(saveBounds.top >= actionBounds.top)
        assertTrue(saveBounds.bottom <= actionBounds.bottom)
        assertFalse(composeRule.textLayoutOf("Save").didOverflowHeight)
    }

    @Test
    fun toastActionHasNoClippingAtFontScale2() {
        val state = TmrToastState()
        setLargeTextContent { TmrToastHost(state) }
        composeRule.settleToast(
            state = state,
            message = "Applied, marked today",
            action = TmrToastAction(label = "Undo", onClick = {}),
        )
        val toastBounds =
            composeRule.onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true).getBoundsInRoot()
        val actionBounds =
            composeRule.onNodeWithTag(TmrChromeTags.TOAST_ACTION, useUnmergedTree = true).getBoundsInRoot()
        val undoBounds = composeRule.onNodeWithText("Undo", useUnmergedTree = true).getBoundsInRoot()
        assertTrue(toastBounds.right <= A11Y_PARENT_WIDTH)
        assertTrue(actionBounds.left >= toastBounds.left)
        assertTrue(actionBounds.right <= toastBounds.right)
        assertTrue(undoBounds.left >= actionBounds.left)
        assertTrue(undoBounds.right <= actionBounds.right)
        assertTrue(undoBounds.top >= actionBounds.top)
        assertTrue(undoBounds.bottom <= actionBounds.bottom)
        val undoLayout = composeRule.textLayoutOf("Undo")
        assertFalse(undoLayout.didOverflowHeight)
    }

    @Test
    fun topBarActionGrowsWithTextAtFontScale3() {
        setLargeTextContent(fontScale = A11Y_STRESS_FONT_SCALE) {
            TmrTopBar(leading = TmrTopBarLeading.Back, onLeading = {}, title = "Resume", action = "Save", onAction = {})
        }
        assertFalse(composeRule.textLayoutOf("Save").didOverflowHeight)
    }

    @Test
    fun toastActionGrowsWithTextAtFontScale3() {
        val state = TmrToastState()
        setLargeTextContent(fontScale = A11Y_STRESS_FONT_SCALE) { TmrToastHost(state) }
        composeRule.settleToast(state, "Applied, marked today", TmrToastAction(label = "Undo", onClick = {}))
        assertFalse(composeRule.textLayoutOf("Undo").didOverflowHeight)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrToastTouchTargetA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun toastActionTouchTargetIsAtLeast48dp() {
        composeRule.mainClock.autoAdvance = false
        val state = TmrToastState()
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrToastHost(state)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.settleToast(
            state = state,
            message = "Applied, marked today",
            action = TmrToastAction(label = "Undo", onClick = {}),
        )
        composeRule
            .onNodeWithTag(TmrChromeTags.TOAST_ACTION, useUnmergedTree = true)
            .assertTouchHeightIsEqualTo(48.dp)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrToastBehaviourA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setHost(
        state: TmrToastState,
        motion: TmrMotion = TmrMotionDefaults.Default,
        recommendedTimeoutMillis: (Int, Int) -> Int = { original, _ -> original },
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides motion) {
                    Box(Modifier.fillMaxSize()) {
                        TmrToastHost(
                            state = state,
                            recommendedTimeoutMillis = recommendedTimeoutMillis,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun actionDismissesBeforeCallback() {
        val state = TmrToastState()
        setHost(state)
        composeRule.settleToast(
            state = state,
            message = "Applied, marked today",
            action = TmrToastAction(label = "Undo") { state.show("Undone") },
        )
        composeRule
            .onNodeWithTag(TmrChromeTags.TOAST_ACTION, useUnmergedTree = true)
            .performClick()
        repeat(2) {
            composeRule.mainClock.advanceTimeBy(A11Y_FRAME_MS)
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("Undone").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("Undone", state.current?.message) }
    }

    @Test
    fun toastWithActionUsesRecommendedTimeout() {
        val state = TmrToastState()
        var recordedOriginal = -1
        var recordedFlags = -1
        setHost(
            state = state,
            recommendedTimeoutMillis = { original, flags ->
                recordedOriginal = original
                recordedFlags = flags
                A11Y_LONG_TIMEOUT_MS
            },
        )
        composeRule.settleToast(
            state = state,
            message = "Applied, marked today",
            action = TmrToastAction(label = "Undo", onClick = {}),
        )
        assertEquals(3200, recordedOriginal)
        assertEquals(
            AccessibilityManager.FLAG_CONTENT_TEXT or AccessibilityManager.FLAG_CONTENT_CONTROLS,
            recordedFlags,
        )
        composeRule.mainClock.advanceTimeBy(A11Y_LONG_TIMEOUT_MS - 1L, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNotNull(state.current) }
        composeRule.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }

        composeRule.settleToast(state = state, message = "Changes saved")
        composeRule.mainClock.advanceTimeBy(A11Y_TOAST_DISPLAY_MS, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }
        assertEquals(3200, recordedOriginal)
    }

    @Test
    fun toastReducedMotionFirstFrameIsFinal() {
        val state = TmrToastState()
        setHost(state, motion = TmrMotionDefaults.Reduced)
        composeRule.settleToast(state = state, message = "Changes saved")
        val toastNode = composeRule.onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true)
        toastNode.assertIsDisplayed()
        val firstTop = toastNode.getBoundsInRoot().top
        assertEquals(A11Y_TOAST_TOP.value, firstTop.value, 0.5f)
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        val settledTop =
            composeRule
                .onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true)
                .getBoundsInRoot()
                .top
        assertEquals(firstTop.value, settledTop.value, 0.5f)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrChromeSemanticsA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tabsAnnouncePosition() {
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrTabBar(
                        selected = TmrTab.Applications,
                        onApplications = {},
                        onAdd = {},
                        onProfile = {},
                    )
                }
            }
        }
        val applications =
            composeRule
                .onNodeWithText("Applications")
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.CollectionItemInfo)
        val profile =
            composeRule
                .onNodeWithText("Profile")
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.CollectionItemInfo)
        assertNotNull(applications)
        assertNotNull(profile)
        assertEquals(0, applications?.columnIndex)
        assertEquals(1, profile?.columnIndex)
    }

    @Test
    fun sheetHasPaneTitleAndBackDismisses() {
        var dismissals = 0
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrBottomSheet(onDismiss = { dismissals++ }, title = "Sheet") {
                        Text("Change status", color = TmrTheme.colors.text)
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
        assertEquals("Sheet", paneTitle)
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.runOnIdle { assertEquals(1, dismissals) }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrChromeGeometryA11yTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var spacing: TmrSpacing? = null

    @Composable
    private fun captureSpacing() {
        spacing = TmrTheme.spacing
    }

    @Test
    fun tabItemKeepsMinimumHeight() {
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrTabBar(
                        selected = TmrTab.Applications,
                        onApplications = {},
                        onAdd = {},
                        onProfile = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("Applications").assertHeightIsAtLeast(54.dp)
    }

    @Test
    fun toastSitsAtGutterAndToastTop() {
        composeRule.mainClock.autoAdvance = false
        val state = TmrToastState()
        composeRule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Reduced) {
                    Box(Modifier.fillMaxSize()) {
                        captureSpacing()
                        TmrToastHost(state)
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.settleToast(state = state, message = "Changes saved")
        val captured = requireNotNull(spacing)
        val root = composeRule.onRoot().getBoundsInRoot()
        val toast =
            composeRule.onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true).getBoundsInRoot()
        assertEquals((captured.gutter + 16.dp).value, (toast.left - root.left).value, 1f)
        assertEquals((captured.gutter + 8.dp).value, (root.right - toast.right).value, 1f)
        assertTrue(toast.top - root.top >= captured.toastTop)
    }

    @Test
    fun sheetHandleSitsAtSheetPaddingTop() {
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    captureSpacing()
                    TmrBottomSheet(onDismiss = {}, title = "Sheet") {
                        Text("Change status", color = TmrTheme.colors.text)
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val captured = requireNotNull(spacing)
        val sheet =
            composeRule.onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true).getBoundsInRoot()
        val handle =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET_HANDLE, useUnmergedTree = true)
                .getBoundsInRoot()
        assertEquals(
            captured.sheetPaddingTop.value,
            (handle.top - sheet.top).value,
            1f,
        )
    }
}
