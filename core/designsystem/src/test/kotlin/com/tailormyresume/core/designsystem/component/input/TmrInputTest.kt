package com.tailormyresume.core.designsystem.component.input

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.TmrDarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrInputTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun buttonFillsComeFromTokens() {
        System.setProperty("robolectric.useEmbeddedViewRoot", "false")
        rule.setContent {
            TmrPreviewTheme {
                Column {
                    TmrPrimaryButton(label = "Enabled", onClick = {}, enabled = true)
                    TmrPrimaryButton(label = "Disabled", onClick = {}, enabled = false)
                    TmrSecondaryButton(label = "Secondary", onClick = {})
                }
            }
        }
        val screen = screenPixels()
        assertEquals(TmrDarkColors.lime.toArgb(), fillOf(screen, "Enabled"))
        assertEquals(TmrDarkColors.disabledFill.toArgb(), fillOf(screen, "Disabled"))
        assertEquals(TmrDarkColors.surfaceHigh.toArgb(), fillOf(screen, "Secondary"))
    }

    @Test
    fun disabledTapRunsOnDisabledClickOnly() {
        var clicks = 0
        var disabledClicks = 0
        rule.setContent {
            TmrPreviewTheme {
                TmrPrimaryButton(
                    label = "Continue",
                    onClick = { clicks++ },
                    enabled = false,
                    onDisabledClick = { disabledClicks++ },
                )
            }
        }
        rule.onNodeWithText("Continue").performClick()
        assertEquals(1, disabledClicks)
        assertEquals(0, clicks)
    }

    @Test
    fun enabledTapRunsOnClick() {
        var clicks = 0
        var disabledClicks = 0
        rule.setContent {
            TmrPreviewTheme {
                TmrPrimaryButton(
                    label = "Continue",
                    onClick = { clicks++ },
                    enabled = true,
                    onDisabledClick = { disabledClicks++ },
                )
            }
        }
        rule.onNodeWithText("Continue").performClick()
        assertEquals(1, clicks)
        assertEquals(0, disabledClicks)
    }

    @Test
    fun disabledReportsButtonWithStateDescription() {
        rule.setContent {
            TmrPreviewTheme {
                TmrPrimaryButton(label = "Continue", onClick = {}, enabled = false)
            }
        }
        val config = rule.onNodeWithText("Continue").fetchSemanticsNode().config
        assertEquals(Role.Button, config.getOrNull(SemanticsProperties.Role))
        assertEquals(
            rule.activity.getString(R.string.core_designsystem_input_not_available),
            config.getOrNull(SemanticsProperties.StateDescription),
        )
    }

    @Test
    fun enabledColoursAndMinHeight() {
        rule.setContent {
            TmrPreviewTheme {
                Column {
                    TmrPrimaryButton(label = "Continue", onClick = {}, enabled = true)
                    TmrPrimaryButton(label = "Later", onClick = {}, enabled = false)
                }
            }
        }
        assertEquals(TmrDarkColors.ink, labelColor("Continue"))
        assertEquals(TmrDarkColors.textDisabled, labelColor("Later"))
        rule.onNodeWithText("Continue").assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun choiceRowRolesAndSelectedState() {
        var clicks = 0
        rule.setContent {
            TmrPreviewTheme {
                Column {
                    TmrChoiceRow(label = "Yes", selected = true, onClick = { clicks++ })
                    TmrChoiceRow(label = "No", selected = false, onClick = { clicks++ })
                }
            }
        }
        val selected = rule.onNodeWithText("Yes").fetchSemanticsNode().config
        val unselected = rule.onNodeWithText("No").fetchSemanticsNode().config
        assertEquals(Role.RadioButton, selected.getOrNull(SemanticsProperties.Role))
        assertEquals(Role.RadioButton, unselected.getOrNull(SemanticsProperties.Role))
        assertEquals(true, selected.getOrNull(SemanticsProperties.Selected))
        assertEquals(false, unselected.getOrNull(SemanticsProperties.Selected))
        rule.onNodeWithText("No").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun toggleSwitchRoleToggleableAndCallback() {
        var captured: Boolean? = null
        rule.setContent {
            TmrPreviewTheme {
                Column {
                    TmrToggle(
                        label = "Notify me",
                        checked = false,
                        onCheckedChange = { captured = it },
                        modifier = Modifier.testTag("t"),
                    )
                    TmrToggle(
                        label = "Weekly digest",
                        checked = true,
                        onCheckedChange = { captured = it },
                        modifier = Modifier.testTag("u"),
                    )
                }
            }
        }
        val off = rule.onNodeWithTag("t").fetchSemanticsNode().config
        assertEquals(Role.Switch, off.getOrNull(SemanticsProperties.Role))
        assertEquals(ToggleableState.Off, off.getOrNull(SemanticsProperties.ToggleableState))
        val on = rule.onNodeWithTag("u").fetchSemanticsNode().config
        assertEquals(Role.Switch, on.getOrNull(SemanticsProperties.Role))
        assertEquals(ToggleableState.On, on.getOrNull(SemanticsProperties.ToggleableState))
        rule.onNodeWithTag("t").performClick()
        assertEquals(true, captured)
    }

    @Test
    fun toggleRowIsTheSwitchWithNameAndTouchTarget() {
        var captured: Boolean? = null
        rule.setContent {
            TmrPreviewTheme {
                TmrToggle(
                    label = "Notify me",
                    checked = false,
                    onCheckedChange = { captured = it },
                    modifier = Modifier.testTag("t"),
                )
            }
        }
        val row = rule.onNodeWithTag("t")
        assertEquals(Role.Switch, row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Role))
        row.assertHeightIsAtLeast(48.dp)
        row.assert(hasText("Notify me"))
        rule.onNodeWithText("Notify me", useUnmergedTree = false).assertHeightIsAtLeast(48.dp)
        rule.onNode(hasText("Notify me") and isToggleable()).performClick()
        assertEquals(true, captured)
    }

    @Test
    fun toggleExposesOneToggleableNode() {
        rule.setContent {
            TmrPreviewTheme {
                TmrToggle(
                    label = "Notify me",
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("t"),
                )
            }
        }
        rule.onAllNodes(isToggleable()).assertCountEquals(1)
    }

    @Test
    fun textFieldLabelIsAccessibleNameAndEditable() {
        var typed: String? = null
        rule.setContent {
            TmrPreviewTheme {
                TmrTextField(value = "", onValueChange = { typed = it }, label = "Email")
            }
        }
        rule.onNodeWithContentDescription("Email").performTextInput("a")
        assertEquals("a", typed)
        rule.onNodeWithText("Email").assertIsDisplayed()
    }

    @Test
    fun readOnlyFieldRejectsInput() {
        var typed: String? = null
        rule.setContent {
            TmrPreviewTheme {
                TmrTextField(value = "x", onValueChange = { typed = it }, label = "Email", readOnly = true)
            }
        }
        val config = rule.onNodeWithContentDescription("Email").fetchSemanticsNode().config
        assertNull(config.getOrNull(SemanticsActions.SetText))
        assertTrue(typed == null)
    }

    private fun labelColor(text: String): Color {
        val action = rule.onNodeWithText(text).fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
        val results = mutableListOf<TextLayoutResult>()
        action?.action?.invoke(results)
        return results.first().layoutInput.style.color
    }

    @Test
    fun toggleLabelFirstGlyphIsNotClippedAtFontScale200() {
        System.setProperty("robolectric.useEmbeddedViewRoot", "false")
        val label = "HHHH HHHH HHHH HHHH HHHH HHHH HHHH HHHH"
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                TmrPreviewTheme {
                    Box(modifier = Modifier.width(374.dp)) {
                        TmrToggle(label = label, checked = false, onCheckedChange = {})
                    }
                }
            }
        }
        val screen = screenPixels()
        val text = rule.onNode(hasText(label), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        text.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val layout = layouts.first()
        assertTrue("lineCount ${layout.lineCount}", layout.lineCount >= 2)
        val bounds = text.boundsInRoot
        val background = screen.getPixel(0, 0)
        fun inkLeft(fromFraction: Float, toFraction: Float): Int {
            val lineTop = layout.getLineTop(0)
            val lineHeight = layout.getLineBottom(0) - lineTop
            val rows = (bounds.top + lineTop + lineHeight * fromFraction).toInt()..(bounds.top + lineTop + lineHeight * toFraction).toInt()
            return rows.minOf { y ->
                (bounds.left.toInt()..bounds.left.toInt() + GLYPH_SCAN_PX).first { x -> screen.getPixel(x, y) != background }
            }
        }
        assertEquals(inkLeft(MID_BAND_START, MID_BAND_END), inkLeft(TOP_BAND_START, TOP_BAND_END))
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun screenPixels(): android.graphics.Bitmap {
        val file = folder.newFile("buttons.png")
        captureScreenRoboImage(file.path, RoborazziOptions(taskType = RoborazziTaskType.Record))
        return BitmapFactory.decodeFile(file.path)
    }

    private fun fillOf(screen: android.graphics.Bitmap, text: String): Int {
        val bounds = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        val x = (bounds.left + FILL_SAMPLE_INSET_PX).toInt()
        val y = bounds.center.y.toInt()
        return screen.getPixel(x, y)
    }

    private companion object {
        const val FILL_SAMPLE_INSET_PX = 12f
        const val GLYPH_SCAN_PX = 40
        const val TOP_BAND_START = 0.25f
        const val TOP_BAND_END = 0.32f
        const val MID_BAND_START = 0.55f
        const val MID_BAND_END = 0.62f
    }
}
