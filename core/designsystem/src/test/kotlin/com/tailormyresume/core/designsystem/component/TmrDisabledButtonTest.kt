package com.tailormyresume.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrDarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrDisabledButtonTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun disabledTextButtonLabelUsesDisabledContentWithReadableContrast() {
        rule.setContent {
            TmrPreviewTheme {
                Column {
                    TmrTextButton(label = "Text on", onClick = {}, enabled = true)
                    TmrTextButton(label = "Text off", onClick = {}, enabled = false)
                }
            }
        }
        val enabled = labelColor("Text on")
        val disabled = labelColor("Text off")
        assertEquals(TmrDarkColors.primary, enabled)
        assertEquals(TmrDarkColors.disabledContent, disabled)
        assertTrue(contrast(disabled, TmrDarkColors.card) >= 4.5f)
    }

    @Test
    fun disabledOutlineButtonLabelDiffersFromEnabledAndKeepsPlaceholderContrast() {
        rule.setContent {
            TmrPreviewTheme {
                Column {
                    TmrOutlineButton(label = "Outline on", onClick = {}, enabled = true)
                    TmrOutlineButton(label = "Outline off", onClick = {}, enabled = false)
                }
            }
        }
        val enabled = labelColor("Outline on")
        val disabled = labelColor("Outline off")
        assertEquals(TmrDarkColors.onSurface, enabled)
        assertEquals(TmrDarkColors.onSurfaceVariant, disabled)
        assertTrue(contrast(disabled, TmrDarkColors.primaryContainer) >= 4.5f)
    }

    private fun labelColor(text: String): Color {
        val action = rule.onNodeWithText(text).fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
        val results = mutableListOf<TextLayoutResult>()
        action?.action?.invoke(results)
        return results.first().layoutInput.style.color
    }

    private fun contrast(first: Color, second: Color): Float {
        val brighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (brighter + 0.05f) / (darker + 0.05f)
    }
}
