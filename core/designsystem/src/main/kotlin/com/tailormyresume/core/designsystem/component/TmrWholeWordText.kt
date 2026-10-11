package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val ZERO_WIDTH_SPACE = '\u200B'

private const val SHRINK_STEP = 0.9f

internal class WholeWordAutoSize(
    private val start: TextUnit,
    private val floor: TextUnit,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        var size = start
        while (size > floor && breaksInsideWord(performLayout(constraints, text, size))) {
            size = (size.value * SHRINK_STEP).sp.let { if (it < floor) floor else it }
        }
        return size
    }

    override fun equals(other: Any?): Boolean =
        other is WholeWordAutoSize && other.start == start && other.floor == floor

    override fun hashCode(): Int = 31 * start.hashCode() + floor.hashCode()
}

internal fun Modifier.readAs(text: String): Modifier = semantics { this.text = AnnotatedString(text) }

internal fun withBreakOpportunities(text: String, breakAfter: CharArray): String = buildString {
    text.forEach { char ->
        append(char)
        if (char in breakAfter) append(ZERO_WIDTH_SPACE)
    }
}

internal fun breaksInsideWord(layout: TextLayoutResult): Boolean {
    val text = layout.layoutInput.text.text
    return (0 until layout.lineCount - 1).any { line ->
        val end = layout.getLineEnd(line, visibleEnd = false)
        end in 1 until text.length && !text[end - 1].isBreakBoundary() && !text[end].isWhitespace()
    }
}

private fun Char.isBreakBoundary() = isWhitespace() || this == ZERO_WIDTH_SPACE || this == '-'
