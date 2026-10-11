package com.tailormyresume.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

internal const val NARROW_QUALIFIERS = "w337dp-h734dp-normal-long-notround-any-480dpi-keyshidden-nonav"

internal val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

internal fun ComposeContentTestRule.setTmrContent(
    device: TmrTestDevice,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, device.fontScale)) {
            TmrTheme {
                Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                    content()
                }
            }
        }
    }
}

internal fun ComposeContentTestRule.capture(name: String, device: TmrTestDevice) {
    runBlocking {
        captureForDevice(outputDirectory = "src/test/screenshots", screenName = name, device = device)
    }
}

internal fun ComposeContentTestRule.assertTexts(vararg texts: String) {
    texts.forEach { text ->
        assertTrue(
            "$text is missing",
            onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty(),
        )
    }
}

internal fun ComposeContentTestRule.assertNoTruncatedText() {
    textLayouts().forEach { (node, layout) ->
        val text = layout.layoutInput.text.text
        val widestLine = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
        assertTrue("$text overflows its width: line right $widestLine > ${layout.size.width}", widestLine <= layout.size.width + 1.5f)
        assertFalse("$text overflows its height", layout.didOverflowHeight)
        assertTrue("$text is clipped by its node", layout.size.width <= node.size.width && layout.size.height <= node.size.height)
        val ellipsized = (0 until layout.lineCount).filter { layout.isLineEllipsized(it) }
        assertTrue("$text has ellipsized lines $ellipsized", ellipsized.isEmpty())
    }
}

internal fun ComposeContentTestRule.assertNoWordBrokenAcrossLines() {
    textLayouts().forEach { (_, layout) ->
        val text = layout.layoutInput.text.text
        if (text.contains('@')) return@forEach
        (0 until layout.lineCount - 1).forEach { line ->
            val end = layout.getLineEnd(line, visibleEnd = false)
            val breaksInsideWord = end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
            assertFalse("$text breaks inside a word at $end", breaksInsideWord)
        }
    }
}

private fun ComposeContentTestRule.textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> =
    onAllNodes(SemanticsMatcher("has text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) })
        .fetchSemanticsNodes()
        .mapNotNull { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.firstOrNull()?.let { node to it }
        }
