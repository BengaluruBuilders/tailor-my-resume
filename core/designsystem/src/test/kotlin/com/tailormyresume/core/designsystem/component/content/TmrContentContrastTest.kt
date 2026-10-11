package com.tailormyresume.core.designsystem.component.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrDarkColors
import org.junit.Assert.assertTrue
import org.junit.Test

class TmrContentContrastTest {
    private data class TextOnSurface(val name: String, val text: Color, val surface: Color)

    private fun contrast(first: Color, second: Color): Float {
        val brighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (brighter + 0.05f) / (darker + 0.05f)
    }

    private fun pairs(colors: TmrColors): List<TextOnSurface> {
        val surfaces = listOf("page" to colors.background, "raised card" to colors.surfaceRaised)
        val result = mutableListOf<TextOnSurface>()
        TmrProgressState.entries.forEach { state ->
            val row = tmrProgressRowColors(state, colors)
            result += TextOnSurface("progress $state label", row.label, colors.surfaceRaised)
            result += TextOnSurface("progress $state meta", row.meta, colors.surfaceRaised)
        }
        result += TextOnSurface("percent", colors.lime, colors.background)
        TmrApplicationStatus.entries.forEach { status ->
            val chip = tmrStatusChipColors(status, colors)
            surfaces.forEach { (surfaceName, surface) ->
                result += TextOnSurface("status $status on $surfaceName", chip.text, chip.fill.compositeOver(surface))
            }
        }
        TmrKeywordState.entries.forEach { state ->
            val chip = tmrKeywordChipStyle(state, colors)
            surfaces.forEach { (surfaceName, surface) ->
                result += TextOnSurface("keyword $state on $surfaceName", chip.text, chip.fill.compositeOver(surface))
            }
        }
        TmrSource.entries.forEach { source ->
            surfaces.forEach { (surfaceName, surface) ->
                result += TextOnSurface("source $source on $surfaceName", tmrSourceColor(source, colors), surface)
            }
        }
        TmrTag.entries.forEach { tag ->
            val chip = tmrTagStyle(tag, colors)
            surfaces.forEach { (surfaceName, surface) ->
                result += TextOnSurface("tag $tag on $surfaceName", chip.text, chip.fill.compositeOver(surface))
            }
        }
        surfaces.forEach { (surfaceName, surface) ->
            result += TextOnSurface("list row label on $surfaceName", colors.text, surface)
            result += TextOnSurface("list row meta on $surfaceName", colors.textMuted, surface)
            result += TextOnSurface("coverage label on $surfaceName", colors.textMuted, surface)
            result += TextOnSurface("coverage now on $surfaceName", colors.textMuted, surface)
            result += TextOnSurface("coverage up to on $surfaceName", colors.lime, surface)
            result += TextOnSurface("section label on $surfaceName", colors.textMuted, surface)
        }
        result += TextOnSurface("file card meta", colors.textMuted, colors.surfaceRaised)
        result += TextOnSurface("file card name", colors.text, colors.surfaceRaised)
        TmrApplicationStatus.entries.forEach { status ->
            result += TextOnSurface("status bar legend $status", colors.textSecondary, colors.background)
        }
        result += TextOnSurface("paper badge", colors.lime, colors.ink)
        val paperBackgrounds = listOf(
            colors.paper,
            tmrPaperHighlightColor(TmrPaperHighlight.FromResume, colors),
            tmrPaperHighlightColor(TmrPaperHighlight.FromAnswer, colors),
        )
        paperBackgrounds.forEachIndexed { index, background ->
            result += TextOnSurface("paper body $index", colors.ink, background)
        }
        listOf("lime" to colors.lime, "blue" to colors.blue, "amber" to colors.amber).forEach { (name, disc) ->
            result += TextOnSurface("initial disc on $name", colors.ink, disc)
        }
        result += TextOnSurface("file card button", colors.text, colors.fill)
        result += TextOnSurface("paper legend", colors.textMuted, colors.background)
        return result
    }

    @Test
    fun everyContentTextAndSurfacePairMeetsNormalTextContrast() {
        pairs(TmrDarkColors).forEach { pair ->
            val ratio = contrast(pair.text, pair.surface)
            assertTrue("${pair.name} contrast was $ratio", ratio >= 4.5f)
        }
    }
}
