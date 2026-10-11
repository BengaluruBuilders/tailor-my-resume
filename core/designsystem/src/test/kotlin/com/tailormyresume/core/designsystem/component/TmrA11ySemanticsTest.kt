package com.tailormyresume.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrA11ySemanticsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun innerHeaderTitleIsHeading() {
        rule.setContent {
            TmrPreviewTheme {
                TmrInnerHeader(title = "Fit for Kestrel Labs")
            }
        }
        rule
            .onNodeWithText("Fit for Kestrel Labs")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
    }

    @Test
    fun innerHeaderLongTitleWrapsAtLargeFontScale() {
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Box(Modifier.width(320.dp)) {
                        TmrInnerHeader(
                            title = "Fit for Kestrel Labs Senior Android Platform Engineer",
                            onBack = {},
                            backContentDescription = "Back",
                        )
                    }
                }
            }
        }
        val layout =
            rule
                .onNodeWithText("Fit for Kestrel Labs Senior Android Platform Engineer")
                .textLayoutResult()
        assertTrue(layout.lineCount > 1)
        assertFalse(layout.hasVisualOverflow)
    }

    @Test
    fun singleSelectChipIsRadioButton() {
        rule.setContent {
            TmrPreviewTheme {
                TmrFilterChip(label = "Skills", selected = true, onClick = {}, singleSelect = true)
            }
        }
        rule
            .onNodeWithText("Skills")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun multiSelectChipStaysCheckbox() {
        rule.setContent {
            TmrPreviewTheme {
                TmrFilterChip(label = "Skills", selected = true, onClick = {})
            }
        }
        rule
            .onNodeWithText("Skills")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun longChipLabelWraps() {
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Box(Modifier.width(160.dp)) {
                        TmrFilterChip(label = "Quantified achievements and metrics", selected = false, onClick = {})
                    }
                }
            }
        }
        val layout = rule.onNodeWithText("Quantified achievements and metrics").textLayoutResult()
        assertTrue(layout.lineCount > 1)
    }

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val action =
            fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
                ?: error("SemanticsActions.GetTextLayoutResult is missing")
        val results = mutableListOf<TextLayoutResult>()
        action.action?.invoke(results)
        return results.first()
    }
}
