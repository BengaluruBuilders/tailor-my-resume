package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrDividerStyle { Hairline, Dashed }

@Composable
fun TmrDivider(
    modifier: Modifier = Modifier,
    style: TmrDividerStyle = TmrDividerStyle.Hairline,
    thickness: Dp = TmrWidthHairline,
    color: Color? = null,
) {
    val lineColor = color ?: TmrTheme.colors.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness),
    ) {
        val y = size.height / 2f
        val dash = size.height * 4f
        val effect = if (style == TmrDividerStyle.Dashed) {
            PathEffect.dashPathEffect(intervals = floatArrayOf(dash, dash))
        } else {
            null
        }
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = size.height,
            pathEffect = effect,
        )
    }
}

private const val TMR_DIVIDER_SAMPLE_TITLE = "Original"
private const val TMR_DIVIDER_SAMPLE_BODY = "Wrote weekly SQL reports in PostgreSQL."

@Preview(showBackground = true)
@Composable
private fun TmrDividerDarkPreview() {
    TmrPreviewTheme {
        TmrDividerPreviewColumn()
    }
}

@Composable
private fun TmrDividerPreviewColumn() {
    Column(modifier = Modifier.padding(TmrTheme.spacing.lg)) {
        Text(text = TMR_DIVIDER_SAMPLE_TITLE, style = TmrTheme.typography.labelM)
        TmrDivider()
        Text(text = TMR_DIVIDER_SAMPLE_BODY, style = TmrTheme.typography.bodyL)
        TmrDivider(style = TmrDividerStyle.Dashed)
    }
}
