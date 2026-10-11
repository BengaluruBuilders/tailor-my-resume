package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TmrTheme.colors.onSurface,
    containerColor: Color = TmrTheme.colors.primaryContainer,
    borderColor: Color = Color.Transparent,
    shape: Shape = TmrTheme.shapes.pill,
    size: Dp = TmrHeightTouch,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .size(size)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        shape = shape,
        color = containerColor,
        border = if (borderColor == Color.Transparent) null else BorderStroke(TmrWidthStroke, borderColor),
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(TmrSizeIconControl),
            )
        }
    }
}

@Composable
fun TmrHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TmrIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        tint = TmrTheme.colors.onHeaderControl,
        containerColor = TmrTheme.colors.headerControl,
        borderColor = Color.Transparent,
    )
}

@Composable
fun TmrBackButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onHeader: Boolean = false,
) {
    if (onHeader) {
        TmrHeaderIconButton(icon = TmrIcons.Back, contentDescription = contentDescription, onClick = onClick, modifier = modifier)
    } else {
        TmrIconButton(
            icon = TmrIcons.Back,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

private const val DISABLED_ALPHA = 0.38f

@Preview(showBackground = true)
@Composable
private fun TmrIconButtonDarkPreview() {
    TmrPreviewTheme { TmrIconButtonPreviewRow() }
}

@Composable
private fun TmrIconButtonPreviewRow() {
    Row(
        modifier = Modifier.padding(TmrTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
    ) {
        TmrIconButton(icon = TmrIcons.Add, contentDescription = "Add", onClick = {})
        TmrIconButton(icon = TmrIcons.Delete, contentDescription = "Delete", onClick = {}, enabled = false)
        TmrIconButton(
            icon = TmrIcons.Share,
            contentDescription = "Share",
            onClick = {},
            containerColor = TmrTheme.colors.primaryContainer,
            tint = TmrTheme.colors.onPrimaryContainer,
            borderColor = Color.Transparent,
        )
    }
}
