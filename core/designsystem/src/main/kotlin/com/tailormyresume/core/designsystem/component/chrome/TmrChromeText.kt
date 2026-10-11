package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val SHRINK_STEP = 0.9f

private fun TextLayoutResult.splitsWord(): Boolean {
    val source = layoutInput.text.text
    return (0 until lineCount - 1).any { line ->
        val end = getLineEnd(line)
        !source[end - 1].isWhitespace() && !source[end].isWhitespace()
    }
}

private class WholeWordAutoSize(
    private val start: TextUnit,
    private val floor: TextUnit,
    private val fitTexts: List<AnnotatedString>,
    private val textMeasurer: TextMeasurer,
    private val style: TextStyle,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        var size = start
        while (size > floor && if (fitTexts.isEmpty()) {
                performLayout(constraints, text, size).splitsWord()
            } else {
                fitTexts.any { textMeasurer.measure(it, style.copy(fontSize = size), constraints = constraints).splitsWord() }
            }
        ) {
            size = (size.value * SHRINK_STEP).sp.let { if (it < floor) floor else it }
        }
        return size
    }

    override fun equals(other: Any?): Boolean =
        other is WholeWordAutoSize && other.start == start && other.floor == floor && other.fitTexts == fitTexts &&
            other.textMeasurer == textMeasurer && other.style == style

    override fun hashCode(): Int = listOf(start, floor, fitTexts, textMeasurer, style).hashCode()
}

@Composable
internal fun TmrCapsText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fitTexts: List<String> = emptyList(),
) {
    val fontScale = LocalDensity.current.fontScale
    val oneXSize = (style.fontSize.value / fontScale).sp
    val textMeasurer = rememberTextMeasurer()
    val autoSize =
        remember(style, fontScale, fitTexts, textMeasurer) {
            if (fontScale > 1f) WholeWordAutoSize(style.fontSize, oneXSize, fitTexts.map { AnnotatedString(it.uppercase()) }, textMeasurer, style) else null
        }
    val layout = remember { arrayOfNulls<TextLayoutResult>(1) }
    key(fontScale) {
        Text(
            text = text.uppercase(),
            style = style,
            color = color,
            autoSize = autoSize,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            onTextLayout = { layout[0] = it },
            modifier =
            modifier.clearAndSetSemantics {
                this.text = AnnotatedString(text)
                getTextLayoutResult { layout[0]?.let(it::add) != null }
            },
        )
    }
}
