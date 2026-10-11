package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.designsystem.theme.tmrShadow

@Composable
fun TmrCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    TmrCardSurface(
        modifier = modifier,
        shape = TmrTheme.shapes.card,
        contentPadding = contentPadding,
        trailingAction = trailingAction,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun TmrHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(TmrTheme.spacing.d16 + TmrTheme.spacing.xxs),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.heroCard
    val source = remember { MutableInteractionSource() }
    val surfaceModifier = (if (onClick == null) modifier else modifier.tmrPressScale(source))
        .fillMaxWidth()
        .tmrShadow(TmrTheme.elevation.hero, shape)
    val border = BorderStroke(TmrWidthHairline, colors.outlineVariant)
    if (onClick == null) {
        Surface(modifier = surfaceModifier, shape = shape, color = colors.document, border = border) {
            TmrCardBody(contentPadding, null, content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            shape = shape,
            color = colors.document,
            border = border,
            interactionSource = source,
        ) {
            TmrCardBody(contentPadding, null, content)
        }
    }
}

@Composable
internal fun TmrCardSurface(
    modifier: Modifier,
    shape: Shape,
    contentPadding: PaddingValues?,
    trailingAction: (@Composable () -> Unit)?,
    onClick: (() -> Unit)? = null,
    fill: Color = TmrTheme.colors.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val padding = contentPadding ?: PaddingValues(TmrTheme.spacing.lg)
    if (onClick == null) {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = fill) {
            TmrCardBody(padding, trailingAction, content)
        }
    } else {
        val source = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.tmrPressScale(source).fillMaxWidth(),
            shape = shape,
            color = fill,
            interactionSource = source,
        ) {
            TmrCardBody(padding, trailingAction, content)
        }
    }
}

@Composable
private fun TmrCardBody(
    padding: PaddingValues,
    trailingAction: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (trailingAction != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                trailingAction()
            }
        }
        content()
    }
}

private const val TMR_CARD_SAMPLE_TITLE = "Android Developer"
private const val TMR_CARD_SAMPLE_BODY = "Kestrel Labs · 1 to 2 years"

@Preview(showBackground = true)
@Composable
private fun TmrCardDarkPreview() {
    TmrPreviewTheme { TmrCardPreviewColumn() }
}

@Composable
private fun TmrCardPreviewColumn() {
    Column(
        modifier = Modifier.padding(TmrTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.lg),
    ) {
        TmrCard {
            Text(TMR_CARD_SAMPLE_TITLE, style = TmrTheme.typography.titleM, color = TmrTheme.colors.onSurface)
            Text(TMR_CARD_SAMPLE_BODY, style = TmrTheme.typography.labelM, color = TmrTheme.colors.onSurfaceVariant)
        }
        TmrHeroCard {
            Text(TMR_CARD_SAMPLE_TITLE, style = TmrTheme.typography.titleL, color = TmrTheme.colors.onSurface)
            Text(TMR_CARD_SAMPLE_BODY, style = TmrTheme.typography.bodyM, color = TmrTheme.colors.body)
        }
    }
}
