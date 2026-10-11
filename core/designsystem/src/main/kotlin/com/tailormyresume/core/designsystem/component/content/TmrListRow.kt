package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrDivider
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val DefaultRowMinHeight = 54.dp

private val ChevronSize = 20.dp

@Composable
fun TmrListRow(
    label: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    tag: TmrTag? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    showDivider: Boolean = true,
    stateDescription: String? = null,
    minHeight: Dp = DefaultRowMinHeight,
) {
    val colors = TmrTheme.colors
    Column(modifier.fillMaxWidth()) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .let { base ->
                if (onClick != null) {
                    base.clickable(role = Role.Button, onClick = onClick)
                } else {
                    base.semantics(mergeDescendants = true) {}
                }
            }
            .semantics { if (stateDescription != null) this.stateDescription = stateDescription }
            .padding(vertical = TmrTheme.spacing.sm)
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.md),
        ) {
            Text(
                text = label,
                style = TmrTheme.typography.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            if (meta != null) {
                Text(text = meta, style = TmrTheme.typography.caption, color = colors.textMuted)
            }
            if (tag != null) {
                TmrTagChip(tag)
            }
            if (showChevron) {
                Icon(
                    imageVector = TmrIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(ChevronSize),
                )
            }
        }
        if (showDivider) {
            TmrDivider(color = colors.line)
        }
    }
}
