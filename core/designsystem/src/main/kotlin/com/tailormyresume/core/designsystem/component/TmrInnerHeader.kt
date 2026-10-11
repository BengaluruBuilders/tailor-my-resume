package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrInnerHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "",
    backIcon: ImageVector = TmrIcons.Back,
    trailing: (@Composable () -> Unit)? = null,
    belowTitle: (@Composable () -> Unit)? = null,
    overlap: Dp = 0.dp,
) {
    val colors = TmrTheme.colors
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layoutId(TmrHeaderData(drawsAboveContent = false, overlap = overlap))
            .tmrHeaderBackdrop(colors.header, null)
            .heightIn(min = TmrHeightInnerHeader + statusTop),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusTop, start = TmrTheme.spacing.md, end = TmrTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TmrHeaderIconButton(icon = backIcon, contentDescription = backContentDescription, onClick = onBack)
            }
            TmrInnerHeaderTitle(title, subtitle, belowTitle, Modifier.weight(1f))
            Box(modifier = Modifier.defaultMinSize(minWidth = TmrHeightTouch), contentAlignment = Alignment.CenterEnd) {
                if (trailing != null) {
                    trailing()
                }
            }
        }
    }
}

@Composable
private fun TmrInnerHeaderTitle(
    title: String,
    subtitle: String?,
    belowTitle: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = TmrTheme.spacing.sm),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = TmrTheme.typography.titleL,
            color = TmrTheme.colors.onHeader,
            textAlign = TextAlign.Start,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = TmrTheme.typography.bodyS,
                color = TmrTheme.colors.onHeaderVariant,
                textAlign = TextAlign.Start,
            )
        }
        if (belowTitle != null) {
            Box(modifier = Modifier.padding(top = TmrTheme.spacing.sm)) { belowTitle() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TmrInnerHeaderDarkPreview() {
    TmrPreviewTheme { TmrInnerHeaderSample() }
}

@Composable
private fun TmrInnerHeaderSample() {
    TmrInnerHeader(
        title = "Fit for Kestrel Labs",
        subtitle = "Android Developer · 1 to 2 years",
        onBack = {},
        backContentDescription = "Back",
    )
}
