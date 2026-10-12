package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

data class TmrInputModeOption(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TmrInputModeSelector(
    options: List<TmrInputModeOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TmrTheme.shapes.pill)
            .background(TmrTheme.colors.surfaceHigh)
            .padding(4.dp)
            .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = options.size) },
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(TmrTheme.shapes.pill)
                    .background(if (selected) TmrTheme.colors.text else Color.Transparent, TmrTheme.shapes.pill)
                    .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { collectionItemInfo = CollectionItemInfo(0, 1, index, 1) },
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(option.icon, contentDescription = null, tint = if (selected) TmrTheme.colors.ink else TmrTheme.colors.textSecondary)
                Text(
                    text = option.label,
                    style = TmrTheme.typography.strongLarge,
                    color = if (selected) TmrTheme.colors.ink else TmrTheme.colors.textSecondary,
                )
            }
        }
    }
}
