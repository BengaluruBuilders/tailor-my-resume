package com.tailormyresume.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Immutable
data class TmrFilterChipColors(
    val selectedContainer: Color,
    val onSelectedContainer: Color,
    val container: Color,
    val onContainer: Color,
    val border: Color,
    val selectedBorder: Color,
)

@Composable
fun TmrFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    count: Int? = null,
    singleSelect: Boolean = false,
    colors: TmrFilterChipColors = TmrFilterChipDefaults.colors(),
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.selectedContainer else colors.container,
        animationSpec = TmrTheme.motion.proofSpecs.color,
        label = "tmrFilterChipContainer",
    )
    val contentColor = if (selected) colors.onSelectedContainer else colors.onContainer
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = TmrFilterChipDefaults.height())
            .selectable(
                selected = selected,
                enabled = enabled,
                role = if (singleSelect) Role.RadioButton else Role.Checkbox,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        shape = TmrTheme.shapes.pill,
        color = containerColor,
        border = BorderStroke(
            width = if (selected) TmrWidthStroke else TmrWidthHairline,
            color = if (selected) colors.selectedBorder else colors.border,
        ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = TmrFilterChipDefaults.SelectedContainerHeight)
                .padding(
                    horizontal = TmrSpacingFourteen,
                    vertical = TmrTheme.spacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.xs),
        ) {
            val glyph = if (selected) TmrFilterChipDefaults.selectedTick() else leadingIcon
            if (glyph != null) {
                Icon(
                    imageVector = glyph,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(TmrSizeChipIcon),
                )
            }
            Text(
                text = label,
                style = TmrTheme.typography.labelL.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = TmrTheme.typography.labelL,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

object TmrFilterChipDefaults {
    val SelectedContainerHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = TmrTheme.spacing.xs

    @Composable
    fun colors(
        selectedContainer: Color = TmrTheme.colors.inverseSurface,
        onSelectedContainer: Color = TmrTheme.colors.inverseOnSurface,
        container: Color = TmrTheme.colors.card,
        onContainer: Color = TmrTheme.colors.onSurface,
        border: Color = TmrTheme.colors.outlineVariant,
        selectedBorder: Color = TmrTheme.colors.inverseSurface,
    ): TmrFilterChipColors = TmrFilterChipColors(
        selectedContainer = selectedContainer,
        onSelectedContainer = onSelectedContainer,
        container = container,
        onContainer = onContainer,
        border = border,
        selectedBorder = selectedBorder,
    )

    @Composable
    @ReadOnlyComposable
    fun height(): Dp = TmrHeightChipRow

    @Composable
    @ReadOnlyComposable
    fun selectedTick(): ImageVector = TmrIcons.Check
}

@Preview(showBackground = true)
@Composable
private fun TmrFilterChipDarkPreview() {
    TmrPreviewTheme {
        TmrFilterChipPreviewRow()
    }
}

@Composable
private fun TmrFilterChipPreviewRow() {
    Row(
        modifier = Modifier.padding(TmrTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TmrFilterChip(
            label = "Remote",
            selected = false,
            onClick = {},
        )
        TmrFilterChip(
            label = "Remote",
            selected = true,
            onClick = {},
        )
        TmrFilterChip(
            label = "Hybrid",
            selected = false,
            onClick = {},
            leadingIcon = TmrIcons.Applications,
        )
        TmrFilterChip(
            label = "Hybrid",
            selected = true,
            onClick = {},
            leadingIcon = TmrIcons.Applications,
        )
        TmrFilterChip(
            label = "Offers",
            selected = true,
            onClick = {},
            count = 12,
        )
        TmrFilterChip(
            label = "Offers",
            selected = false,
            onClick = {},
            count = 12,
        )
    }
}
