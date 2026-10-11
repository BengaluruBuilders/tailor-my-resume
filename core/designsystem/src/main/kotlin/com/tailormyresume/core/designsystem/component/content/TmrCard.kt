package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TmrTheme.colors.surfaceRaised, TmrTheme.shapes.card)
            .padding(TmrTheme.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.md),
        content = content,
    )
}
