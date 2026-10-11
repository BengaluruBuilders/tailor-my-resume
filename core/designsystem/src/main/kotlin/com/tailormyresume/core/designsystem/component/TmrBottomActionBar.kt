package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

internal const val TMR_STACKED_FONT_SCALE: Float = 1.5f

@Composable
fun TmrBottomActionBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    creditDisclosure: (@Composable () -> Unit)? = null,
    stacked: Boolean = LocalDensity.current.fontScale >= TMR_STACKED_FONT_SCALE,
    primaryLast: Boolean = true,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().background(TmrTheme.colors.background)) {
        TmrDivider()
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(contentPadding ?: PaddingValues(horizontal = TmrTheme.spacing.gutter))
                .padding(top = TmrTheme.spacing.md, bottom = TmrOuterGapBar),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
        ) {
            if (creditDisclosure != null) {
                creditDisclosure()
            }
            if (stacked) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TmrBarGap),
                    verticalArrangement = if (primaryLast) {
                        ReversedStack(TmrBarGap)
                    } else {
                        Arrangement.spacedBy(TmrBarGap)
                    },
                    maxItemsInEachRow = 1,
                    content = { actions() },
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TmrBarGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
fun TmrIconActionBar(
    secondaryIcon: ImageVector,
    secondaryContentDescription: String,
    onSecondaryClick: () -> Unit,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryTrailingIcon: ImageVector? = null,
    secondaryBadge: String? = null,
    ink: Boolean = false,
) {
    TmrBottomActionBar(modifier = modifier, stacked = false) {
        Box {
            TmrIconButton(
                icon = secondaryIcon,
                contentDescription = secondaryContentDescription,
                onClick = onSecondaryClick,
                containerColor = TmrTheme.colors.card,
                size = TmrHeightButtonLarge,
            )
            if (secondaryBadge != null) {
                TmrActionBadge(secondaryBadge, Modifier.align(Alignment.TopEnd))
            }
        }
        val primaryModifier = Modifier.weight(1f)
        if (ink) {
            TmrInkButton(primaryLabel, onPrimaryClick, primaryModifier, primaryEnabled, trailingIcon = primaryTrailingIcon)
        } else {
            TmrPrimaryButton(primaryLabel, onPrimaryClick, primaryModifier, primaryEnabled, trailingIcon = primaryTrailingIcon)
        }
    }
}

@Composable
private fun TmrActionBadge(text: String, modifier: Modifier) {
    val colors = TmrTheme.colors
    Box(
        modifier = modifier
            .offset(x = 2.dp, y = (-2).dp)
            .defaultMinSize(minWidth = TmrBadgeSize, minHeight = TmrBadgeSize)
            .background(colors.special, TmrTheme.shapes.pill)
            .border(TmrWidthStrokeFocus, colors.background, TmrTheme.shapes.pill)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TmrTheme.typography.labelM.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.onSpecial,
        )
    }
}

private val TmrBarGap = 10.dp
private val TmrBadgeSize = 22.dp

private class ReversedStack(override val spacing: Dp) : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val gap = spacing.roundToPx()
        var position = 0
        for (index in sizes.indices.reversed()) {
            outPositions[index] = position
            position += sizes[index] + gap
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TmrBottomActionBarDarkPreview() {
    TmrPreviewTheme { TmrBottomActionBarSample() }
}

@Composable
private fun TmrBottomActionBarSample() {
    TmrBottomActionBar(
        creditDisclosure = {
            Text(
                text = "Uses 1 of your 4 credits",
                style = TmrTheme.typography.labelM,
                color = TmrTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
    ) {
        TmrOutlinedButton(onClick = {}, modifier = Modifier.weight(1f), text = { Text("Preview") })
        TmrButton(onClick = {}, modifier = Modifier.weight(1f), text = { Text("Export PDF") })
    }
}
