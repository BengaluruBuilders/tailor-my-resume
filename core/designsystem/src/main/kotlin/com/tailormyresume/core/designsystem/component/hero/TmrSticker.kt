package com.tailormyresume.core.designsystem.component.hero

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrSticker(
    text: String,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
    onCheek: Boolean = false,
) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.pill
    Text(
        text = text,
        style = TmrTheme.typography.mono14,
        color = colors.ink,
        modifier = modifier
            .graphicsLayer { rotationZ = rotationDegrees }
            .background(if (onCheek) colors.cheek else colors.paper, shape)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
