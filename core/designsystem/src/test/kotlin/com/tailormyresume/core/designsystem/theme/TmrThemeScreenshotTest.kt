package com.tailormyresume.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrThemeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(screenName: String, content: @Composable () -> Unit) {
        composeRule.setContent { TmrPreviewTheme(content) }
        runBlocking {
            composeRule.captureForDevice(
                outputDirectory = SCREENSHOT_DIRECTORY,
                screenName = screenName,
            )
        }
    }

    @Test
    fun typeSpecimen() = capture("theme_type_specimen") { TypeSpecimen() }

    @Test
    fun colourSwatches() = capture("theme_colour_swatches") { ColourSwatches() }

    private companion object {
        const val SCREENSHOT_DIRECTORY = "src/test/screenshots"
    }
}

@Composable
private fun TypeSpecimen() {
    val typography = TmrTheme.typography
    val rows: List<Pair<String, TextStyle>> = listOf(
        "headline" to typography.headline,
        "headlineSmall" to typography.headlineSmall,
        "title" to typography.title,
        "display" to typography.display,
        "displayLarge" to typography.displayLarge,
        "label" to typography.label,
        "labelWide" to typography.labelWide,
        "button" to typography.button,
        "mono15" to typography.mono15,
        "mono14" to typography.mono14,
        "body" to typography.body,
        "bodyLarge" to typography.bodyLarge,
        "bodySmall" to typography.bodySmall,
        "caption" to typography.caption,
        "strongLarge" to typography.strongLarge,
        "strongSmall" to typography.strongSmall,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { (name, style) ->
            Text(text = "$name 0123 Hamburgefons", style = style, color = TmrTheme.colors.text)
        }
    }
}

@Composable
private fun ColourSwatches() {
    val colors = TmrTheme.colors
    val swatches: List<Pair<String, Color>> = listOf(
        "background" to colors.background,
        "surface" to colors.surface,
        "surfaceRaised" to colors.surfaceRaised,
        "surfaceHigh" to colors.surfaceHigh,
        "sheet" to colors.sheet,
        "sheetOption" to colors.sheetOption,
        "uploadCard" to colors.uploadCard,
        "fill" to colors.fill,
        "disabledFill" to colors.disabledFill,
        "line" to colors.line,
        "lineStrong" to colors.lineStrong,
        "lineHigher" to colors.lineHigher,
        "tabDivider" to colors.tabDivider,
        "text" to colors.text,
        "textSecondary" to colors.textSecondary,
        "textMuted" to colors.textMuted,
        "textDisabled" to colors.textDisabled,
        "ink" to colors.ink,
        "lime" to colors.lime,
        "limeSelected" to colors.limeSelected,
        "limeSoft" to colors.limeSoft,
        "amber" to colors.amber,
        "amberHighlight" to colors.amberHighlight,
        "blue" to colors.blue,
        "cheek" to colors.cheek,
        "paper" to colors.paper,
        "paperFold" to colors.paperFold,
        "segOffer" to colors.segOffer,
        "segRejected" to colors.segRejected,
        "rejectedBorder" to colors.rejectedBorder,
        "scrim" to colors.scrim,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        swatches.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (name, color) ->
                    Row(
                        modifier = Modifier.width(166.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).background(color))
                        Text(
                            text = name,
                            style = TmrTheme.typography.caption,
                            color = TmrTheme.colors.text,
                            modifier = Modifier.height(28.dp),
                        )
                    }
                }
            }
        }
    }
}
