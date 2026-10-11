package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = TmrTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(TmrTheme.shapes.pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TmrTheme.typography.mono14,
            color = if (enabled) colors.lime else colors.textDisabled,
        )
    }
}
