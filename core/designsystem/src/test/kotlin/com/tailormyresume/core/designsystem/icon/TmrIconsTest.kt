package com.tailormyresume.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.VectorPath
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TmrIconsTest {
    @Test
    fun promisedIconsExistAndGoogleGHasFourColouredPaths() {
        listOf(
            TmrIcons.ChevronRight, TmrIcons.Gear, TmrIcons.Plus,
            TmrIcons.File, TmrIcons.Open, TmrIcons.GoogleG,
            TmrIcons.Back, TmrIcons.Close, TmrIcons.Check, TmrIcons.Share,
        ).forEach { assertNotNull(it) }

        val g = TmrIcons.GoogleG
        assertEquals(48f, g.viewportWidth)
        assertEquals(48f, g.viewportHeight)
        val fills = g.root.filterIsInstance<VectorPath>().map { (it.fill as SolidColor).value }
        assertEquals(
            listOf(Color(0xFFEA4335), Color(0xFF4285F4), Color(0xFFFBBC05), Color(0xFF34A853)),
            fills,
        )
    }
}
