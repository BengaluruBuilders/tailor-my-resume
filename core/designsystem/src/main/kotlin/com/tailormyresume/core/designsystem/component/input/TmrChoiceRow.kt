package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSheet: Boolean = false,
) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.statusRow
    val fill = when {
        selected -> colors.limeSelected
        onSheet -> colors.sheetOption
        else -> colors.surface
    }
    val outline = when {
        selected -> colors.lime
        onSheet -> colors.lineHigher
        else -> colors.line
    }
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .background(fill, shape)
            .border(BorderStroke(1.5.dp, outline), shape)
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TmrTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(BorderStroke(2.dp, if (selected) colors.lime else outline), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(modifier = Modifier.size(10.dp).background(colors.lime, CircleShape))
            }
        }
    }
}
