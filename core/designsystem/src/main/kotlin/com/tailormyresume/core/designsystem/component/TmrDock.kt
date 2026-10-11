package com.tailormyresume.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.designsystem.theme.tmrShadow

@Composable
fun TmrDock(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                horizontal = TmrTheme.spacing.gutter,
                vertical = TmrDockDefaults.verticalPadding,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TmrDockDefaults.height)
                .tmrShadow(TmrTheme.elevation.dock, TmrDockShape)
                .clip(TmrDockShape)
                .background(TmrTheme.colors.tool)
                .border(TmrWidthHairline, TmrTheme.colors.outlineSoft, TmrDockShape)
                .padding(TmrDockPadding),
            horizontalArrangement = Arrangement.spacedBy(TmrDockGap),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun RowScope.TmrDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val iconOnly = LocalDensity.current.fontScale >= 1.5f
    val colors = TmrTheme.colors
    val tint by animateColorAsState(
        targetValue = if (selected) colors.onBrand else colors.onToolVariant,
        animationSpec = TmrTheme.motion.proofSpecs.color,
        label = "dockTint",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) colors.brand else Color.Transparent,
        animationSpec = TmrTheme.motion.proofSpecs.color,
        label = "dockFill",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .tmrPressScale(interactionSource)
            .clip(TmrDockItemShape)
            .background(fill)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { if (iconOnly) contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TmrDockItemGap, Alignment.CenterVertically),
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) { icon() }
        if (!iconOnly) {
            Text(
                text = label,
                style = TmrTheme.typography.labelM.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                ),
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private val TmrDockPadding = 6.dp
private val TmrDockGap = 4.dp
private val TmrDockItemGap = 3.dp
private val TmrDockShape = RoundedCornerShape(34.dp)
private val TmrDockItemShape = RoundedCornerShape(28.dp)

private val TmrDockIconSize = 22.dp

@Composable
fun TmrDockIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(TmrDockIconSize))
}

@Preview(showBackground = true)
@Composable
private fun TmrDockDarkPreview() {
    TmrPreviewTheme { TmrDockSample() }
}

@Composable
private fun TmrDockSample() {
    TmrDock {
        TmrDockItem(
            selected = true,
            onClick = {},
            label = "Applications",
            icon = { TmrDockIcon(TmrIcons.Applications) },
        )
        TmrDockItem(
            selected = false,
            onClick = {},
            label = "Profile",
            icon = { TmrDockIcon(TmrIcons.Profile) },
        )
        TmrDockItem(
            selected = false,
            onClick = {},
            label = "Settings",
            icon = { TmrDockIcon(TmrIcons.Settings) },
        )
    }
}
