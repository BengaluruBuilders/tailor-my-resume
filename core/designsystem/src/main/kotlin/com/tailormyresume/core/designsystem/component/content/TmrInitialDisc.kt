package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val DiscSize = 46.dp

@Composable
fun TmrInitialDisc(initial: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = DiscSize, minHeight = DiscSize)
            .background(color, TmrTheme.shapes.monogram)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = TmrTheme.typography.mono15.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = TmrTheme.colors.ink,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
