package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.LocalTmrMotion
import com.tailormyresume.core.designsystem.theme.TmrDarkColors
import com.tailormyresume.core.designsystem.theme.TmrMotionDefaults
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val TOAST_VISIBLE_MS = 3200L
private const val FRAME_MS = 16L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrStepBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pillStylesMatchPrototypeSteps() {
        val firstCurrent = tmrStepPillStyle(step = 1, current = 1, colors = TmrDarkColors)
        assertEquals(TmrDarkColors.lime, firstCurrent.container)
        assertEquals(TmrDarkColors.ink, firstCurrent.content)

        val firstDone = tmrStepPillStyle(step = 1, current = 2, colors = TmrDarkColors)
        assertEquals(TmrDarkColors.surfaceHigh, firstDone.container)
        assertEquals(TmrDarkColors.lime, firstDone.content)

        val secondCurrent = tmrStepPillStyle(step = 2, current = 2, colors = TmrDarkColors)
        assertEquals(TmrDarkColors.lime, secondCurrent.container)
        assertEquals(TmrDarkColors.ink, secondCurrent.content)

        val thirdCurrent = tmrStepPillStyle(step = 3, current = 3, colors = TmrDarkColors)
        assertEquals(TmrDarkColors.lime, thirdCurrent.container)
        assertEquals(TmrDarkColors.ink, thirdCurrent.content)

        val secondLater = tmrStepPillStyle(step = 2, current = 1, colors = TmrDarkColors)
        assertEquals(TmrDarkColors.surfaceHigh, secondLater.container)
        assertEquals(TmrDarkColors.textMuted, secondLater.content)

        val thirdLater = tmrStepPillStyle(step = 3, current = 2, colors = TmrDarkColors)
        assertEquals(TmrDarkColors.surfaceHigh, thirdLater.container)
        assertEquals(TmrDarkColors.textMuted, thirdLater.content)
    }

    @Test
    fun stepTextsAndDescription() {
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrStepBar(current = 2)
                }
            }
        }
        composeRule.onNodeWithText("✓ Profile").assertIsDisplayed()
        composeRule.onNodeWithText("2 Job").assertIsDisplayed()
        composeRule.onNodeWithText("3 Tailor").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Step 2 of 3, Job").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrTopBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun backLeadingHasLabelAndTouchTarget() {
        var leadingClicks = 0
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrTopBar(
                        leading = TmrTopBarLeading.Back,
                        onLeading = { leadingClicks++ },
                        title = "Resume",
                        action = "Save",
                        onAction = {},
                    )
                }
            }
        }
        val leading =
            composeRule
                .onNodeWithContentDescription("Back")
                .assertTouchHeightIsEqualTo(48.dp)
                .assertTouchWidthIsEqualTo(48.dp)
        leading.performClick()
        composeRule.runOnIdle { assertEquals(1, leadingClicks) }
    }

    @Test
    fun closeLeadingHasLabelAndTouchTarget() {
        var leadingClicks = 0
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrTopBar(
                        leading = TmrTopBarLeading.Close,
                        onLeading = { leadingClicks++ },
                    )
                }
            }
        }
        composeRule
            .onNodeWithContentDescription("Close")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, leadingClicks) }
    }

    @Test
    fun actionPillHasTouchTargetAndFiresOnce() {
        var actionClicks = 0
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrTopBar(
                        leading = TmrTopBarLeading.Back,
                        onLeading = {},
                        title = "Resume",
                        action = "Save",
                        onAction = { actionClicks++ },
                    )
                }
            }
        }
        composeRule
            .onNodeWithTag(TmrChromeTags.TOP_BAR_ACTION)
            .assertTouchHeightIsEqualTo(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, actionClicks) }
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrTabBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectedExposesSelectedAndCallbacksFire() {
        var applicationsClicks = 0
        var addClicks = 0
        var profileClicks = 0
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrTabBar(
                        selected = TmrTab.Applications,
                        onApplications = { applicationsClicks++ },
                        onAdd = { addClicks++ },
                        onProfile = { profileClicks++ },
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Applications")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        composeRule
            .onNodeWithText("Profile")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        composeRule
            .onNodeWithTag(TmrChromeTags.TAB_ADD, useUnmergedTree = true)
            .assertHeightIsEqualTo(56.dp)

        composeRule.onNodeWithText("Applications").performClick()
        composeRule.runOnIdle { assertEquals(1, applicationsClicks) }

        composeRule
            .onNodeWithTag(TmrChromeTags.TAB_ADD, useUnmergedTree = true)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, addClicks) }

        composeRule.onNodeWithText("Profile").performClick()
        composeRule.runOnIdle {
            assertEquals(1, profileClicks)
            assertEquals(1, applicationsClicks)
            assertEquals(1, addClicks)
        }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrToastTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun host(state: TmrToastState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrToastHost(state)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun show(state: TmrToastState, message: String, action: TmrToastAction? = null) {
        composeRule.runOnIdle { state.show(message, action) }
        repeat(2) {
            composeRule.mainClock.advanceTimeBy(FRAME_MS)
            composeRule.waitForIdle()
        }
    }

    @Test
    fun toastIsVisibleAt3199AndGoneAt3200() {
        val state = TmrToastState()
        host(state)
        show(state, "Changes saved")
        composeRule.mainClock.advanceTimeBy(TOAST_VISIBLE_MS - 1, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNotNull(state.current) }
        composeRule.onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true).assertExists()
        composeRule.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }
        composeRule.mainClock.advanceTimeBy(FRAME_MS)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun secondToastReplacesFirstAndRestartsTimer() {
        val state = TmrToastState()
        host(state)
        show(state, "Changes saved")
        composeRule.mainClock.advanceTimeBy(2000, ignoreFrameDuration = true)
        show(state, "Applied, marked today")
        composeRule.mainClock.advanceTimeBy(2000, ignoreFrameDuration = true)
        composeRule.onNodeWithText("Applied, marked today").assertIsDisplayed()
        composeRule.onNodeWithText("Changes saved").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(TOAST_VISIBLE_MS - 2000, ignoreFrameDuration = true)
        composeRule.runOnIdle { assertNull(state.current) }
    }

    @Test
    fun actionRunsOnce() {
        var actionClicks = 0
        val state = TmrToastState()
        host(state)
        show(
            state,
            "Applied, marked today",
            TmrToastAction(label = "Undo", onClick = { actionClicks++ }),
        )
        composeRule
            .onNodeWithTag(TmrChromeTags.TOAST_ACTION, useUnmergedTree = true)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, actionClicks) }
        composeRule.mainClock.advanceTimeBy(FRAME_MS)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun toastIsPoliteLiveRegion() {
        val state = TmrToastState()
        host(state)
        show(state, "Changes saved")
        composeRule
            .onNodeWithTag(TmrChromeTags.TOAST, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrBottomSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun scrimSheetHandleAndPaddingMatchPrototype() {
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
        composeRule
            .onNodeWithTag(TmrChromeTags.SHEET_HANDLE, useUnmergedTree = true)
            .assertWidthIsEqualTo(44.dp)
            .assertHeightIsEqualTo(4.dp)

        val rootBounds = composeRule.onRoot().getBoundsInRoot()
        val sheetNode = composeRule.onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true)
        val sheetBounds = sheetNode.getBoundsInRoot()
        val contentBounds =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET_CONTENT, useUnmergedTree = true)
                .getBoundsInRoot()

        assertEquals(rootBounds.width, sheetBounds.width)
        assertEquals(rootBounds.bottom, sheetBounds.bottom)
        assertEquals(18.dp, contentBounds.left - sheetBounds.left)

        composeRule
            .onNodeWithTag(TmrChromeTags.SCRIM, useUnmergedTree = true)
            .performTouchInput { click(Offset(center.x, 8f)) }
        composeRule.runOnIdle { assertEquals(1, dismissals) }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrChromeMotionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setSheetContent(motion: Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(
                    LocalTmrMotion provides if (motion) TmrMotionDefaults.Default else TmrMotionDefaults.Reduced,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        TmrBottomSheet(onDismiss = {}, title = "Sheet") {
                            Text("Change status", color = TmrTheme.colors.text)
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun reducedFirstFrameIsAtFinalPosition() {
        setSheetContent(motion = false)
        val rootBounds = composeRule.onRoot().getBoundsInRoot()
        val sheetBounds =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true)
                .getBoundsInRoot()
        assertEquals(rootBounds.bottom, sheetBounds.bottom)
    }

    @Test
    fun motionEnabledStartsBelowFinalPosition() {
        setSheetContent(motion = true)
        val firstFrameTop =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true)
                .getBoundsInRoot()
                .top
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        val finalTop =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true)
                .getBoundsInRoot()
                .top
        val rootBounds = composeRule.onRoot().getBoundsInRoot()
        assertTrue(firstFrameTop > finalTop)
        val finalBottom =
            composeRule
                .onNodeWithTag(TmrChromeTags.SHEET, useUnmergedTree = true)
                .getBoundsInRoot()
                .bottom
        assertEquals(rootBounds.bottom, finalBottom)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrChromeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        screenName: String,
        settleMillis: Long = 0L,
        onComposed: (ComposeTestRule) -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        val fontScale = mutableStateOf(TmrTestDevices.DEFAULT_FONT_SCALE)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale.value),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(TmrTheme.colors.background),
                    ) {
                        content()
                    }
                }
            }
        }
        composeRule.waitForIdle()
        onComposed(composeRule)
        composeRule.waitForIdle()
        if (settleMillis > 0L) {
            composeRule.mainClock.advanceTimeBy(settleMillis)
            composeRule.waitForIdle()
        }
        runBlocking {
            composeRule.captureForDevice(
                outputDirectory = SCREENSHOT_DIRECTORY,
                screenName = screenName,
                device = TmrTestDevices.prototype,
            )
            composeRule.runOnIdle {
                fontScale.value = TmrTestDevices.prototypeLargeFont.fontScale
            }
            repeat(2) {
                composeRule.mainClock.advanceTimeBy(FRAME_MS)
                composeRule.waitForIdle()
            }
            composeRule.captureForDevice(
                outputDirectory = SCREENSHOT_DIRECTORY,
                screenName = screenName,
                device = TmrTestDevices.prototypeLargeFont,
            )
        }
    }

    @Test
    fun topBarBackAction() =
        capture("chrome_topbar_back_action") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrTopBar(
                    leading = TmrTopBarLeading.Back,
                    onLeading = {},
                    title = "Resume",
                    action = "Save",
                    onAction = {},
                )
            }
        }

    @Test
    fun topBarCloseNoAction() =
        capture("chrome_topbar_close_noaction") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrTopBar(
                    leading = TmrTopBarLeading.Close,
                    onLeading = {},
                )
            }
        }

    @Test
    fun tabBarApplications() =
        capture("chrome_tabbar_applications") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrTabBar(
                    selected = TmrTab.Applications,
                    onApplications = {},
                    onAdd = {},
                    onProfile = {},
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

    @Test
    fun tabBarProfile() =
        capture("chrome_tabbar_profile") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrTabBar(
                    selected = TmrTab.Profile,
                    onApplications = {},
                    onAdd = {},
                    onProfile = {},
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

    @Test
    fun toastPlain() {
        val state = TmrToastState()
        capture(
            screenName = "chrome_toast_plain",
            settleMillis = 1000L,
            onComposed = { rule -> rule.runOnIdle { state.show("Changes saved") } },
        ) {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrToastHost(state)
            }
        }
    }

    @Test
    fun toastAction() {
        val state = TmrToastState()
        capture(
            screenName = "chrome_toast_action",
            settleMillis = 1000L,
            onComposed = { rule ->
                rule.runOnIdle {
                    state.show(
                        "Applied, marked today",
                        TmrToastAction(label = "Undo", onClick = {}),
                    )
                }
            },
        ) {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrToastHost(state)
            }
        }
    }

    @Test
    fun stepBarOne() =
        capture("chrome_stepbar_1") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrStepBar(current = 1)
            }
        }

    @Test
    fun stepBarTwo() =
        capture("chrome_stepbar_2") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrStepBar(current = 2)
            }
        }

    @Test
    fun stepBarThree() =
        capture("chrome_stepbar_3") {
            Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                TmrStepBar(current = 3)
            }
        }

    @Test
    fun sheet() =
        capture(screenName = "chrome_sheet", settleMillis = 1000L) {
            TmrBottomSheet(onDismiss = {}, title = "Sheet") {
                Text("Change status", color = TmrTheme.colors.text)
            }
        }

    private companion object {
        const val SCREENSHOT_DIRECTORY = "src/test/screenshots"
    }
}
