package com.tailormyresume.core.designsystem.component.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrHeroColor { Blue, Amber, Lime }

@Composable
fun TmrHeroCard(
    color: TmrHeroColor,
    label: String,
    headline: String,
    modifier: Modifier = Modifier,
    decoration: @Composable BoxScope.() -> Unit = {},
) {
    val colors = TmrTheme.colors
    val fill = when (color) {
        TmrHeroColor.Blue -> colors.blue
        TmrHeroColor.Amber -> colors.amber
        TmrHeroColor.Lime -> colors.lime
    }
    val shape = TmrTheme.shapes.hero
    Box(modifier = modifier.fillMaxWidth().clip(shape).background(fill, shape)) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(text = label.uppercase(), style = TmrTheme.typography.label, color = colors.ink)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = headline,
                style = TmrTheme.typography.headline,
                color = colors.ink,
            )
        }
        decoration()
    }
}
