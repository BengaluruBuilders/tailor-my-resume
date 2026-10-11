package com.tailormyresume.core.designsystem.component.content

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.component.WholeWordAutoSize
import com.tailormyresume.core.designsystem.component.withBreakOpportunities

internal val TmrFitTextLayoutKey = SemanticsPropertyKey<() -> TextLayoutResult?>("TmrFitTextLayout")

@Composable
internal fun TmrFitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    breakAfter: CharArray = charArrayOf(),
) {
    val fontScale = LocalDensity.current.fontScale
    val autoSize = remember(style.fontSize, fontScale) {
        if (fontScale > 1f) WholeWordAutoSize(style.fontSize, (style.fontSize.value / fontScale).sp) else null
    }
    val layout = remember { arrayOfNulls<TextLayoutResult>(1) }
    key(fontScale) {
        Text(
            text = withBreakOpportunities(text, breakAfter),
            style = style,
            color = color,
            autoSize = autoSize,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            onTextLayout = { layout[0] = it },
            modifier = modifier.clearAndSetSemantics {
                this.text = AnnotatedString(text)
                this[TmrFitTextLayoutKey] = { layout[0] }
            },
        )
    }
}
