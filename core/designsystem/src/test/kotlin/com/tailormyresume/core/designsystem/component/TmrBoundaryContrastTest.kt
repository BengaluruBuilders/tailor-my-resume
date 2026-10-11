package com.tailormyresume.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrDarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmrBoundaryContrastTest {
    @Test
    fun boundaryTokenIsThreeToOneOnSurfaceAndCard() {
        listOf("dark" to TmrDarkColors).forEach { (mode, colors) ->
            surfaces(colors).forEach { (name, color) ->
                val ratio = contrast(colors.boundary, color)
                assertTrue("$mode boundary on $name contrast was $ratio", ratio >= 3.0f)
            }
        }
    }

    @Test
    fun boundaryTokenDiffersFromHairlineToken() {
        assertNotEquals(TmrDarkColors.boundary, TmrDarkColors.outlineVariant)
        assertEquals(Color(0xFF3A3D3B), TmrDarkColors.outlineVariant)
    }

    private fun surfaces(colors: TmrColors): List<Pair<String, Color>> = with(colors) {
        listOf(
            "background" to background,
            "surface" to surface,
            "card" to card,
            "sheet" to sheet,
        )
    }

    private fun contrast(first: Color, second: Color): Float {
        val brighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (brighter + 0.05f) / (darker + 0.05f)
    }
}
