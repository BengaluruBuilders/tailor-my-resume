package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrButtonSize { Large, Compact }

enum class TmrButtonKind { Primary, Secondary, Outline, Text, Destructive, DestructiveText, Ink }

object TmrButtonDefaults {
    val ContentPadding = PaddingValues(horizontal = 22.dp)
    val TextContentPadding = PaddingValues(horizontal = 12.dp)
    val IconSize = 20.dp
    val CompactContentPadding = PaddingValues(horizontal = 16.dp)
    val CompactIconSize = 17.dp
}

private class TmrButtonPalette(val container: Color, val content: Color, val border: BorderStroke?)

private fun TmrColors.buttonPalette(
    kind: TmrButtonKind,
    surface: TmrButtonSurface,
    pressed: Boolean,
): TmrButtonPalette = when (kind) {
    TmrButtonKind.Primary -> TmrButtonPalette(if (pressed) brandPressed else brand, onBrand, null)
    TmrButtonKind.Ink -> TmrButtonPalette(inverseSurface, inverseOnSurface, null)
    TmrButtonKind.Secondary -> TmrButtonPalette(primaryContainer, onPrimaryContainer, null)
    TmrButtonKind.Destructive -> TmrButtonPalette(error, onError, null)
    TmrButtonKind.DestructiveText -> TmrButtonPalette(Color.Transparent, error, null)
    TmrButtonKind.Outline -> outlinePalette(surface)
    TmrButtonKind.Text -> textPalette(surface)
}

private fun TmrColors.outlinePalette(surface: TmrButtonSurface): TmrButtonPalette = when (surface) {
    TmrButtonSurface.Header -> TmrButtonPalette(
        Color.Transparent,
        onHeader,
        BorderStroke(TmrWidthStroke, onHeader.copy(alpha = 0.85f)),
    )
    TmrButtonSurface.Default -> TmrButtonPalette(
        Color.Transparent,
        onSurface,
        BorderStroke(TmrWidthStroke, boundary),
    )
}

private fun TmrColors.disabledPalette(kind: TmrButtonKind, surface: TmrButtonSurface): TmrButtonPalette {
    val onHeaderSurface = surface == TmrButtonSurface.Header
    if (kind == TmrButtonKind.Text || kind == TmrButtonKind.DestructiveText) {
        return TmrButtonPalette(Color.Transparent, if (onHeaderSurface) onHeaderVariant else disabledContent, null)
    }
    if (kind == TmrButtonKind.Outline && onHeaderSurface) {
        return TmrButtonPalette(Color.Transparent, onHeaderVariant, BorderStroke(TmrWidthStroke, onHeaderVariant))
    }
    return TmrButtonPalette(primaryContainer, onSurfaceVariant, null)
}

private fun TmrColors.textPalette(surface: TmrButtonSurface): TmrButtonPalette = when (surface) {
    TmrButtonSurface.Header -> TmrButtonPalette(Color.Transparent, onHeader, null)
    TmrButtonSurface.Default -> TmrButtonPalette(Color.Transparent, primary, null)
}

@Composable
internal fun TmrButtonBase(
    kind: TmrButtonKind,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    contentPadding: PaddingValues,
    size: TmrButtonSize = TmrButtonSize.Large,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val minHeight = if (size == TmrButtonSize.Compact) TmrHeightButtonCompact else TmrHeightButtonLarge
    val palette = if (!enabled) {
        TmrTheme.colors.disabledPalette(kind, LocalTmrButtonSurface.current)
    } else {
        TmrTheme.colors.buttonPalette(kind, LocalTmrButtonSurface.current, pressed)
    }
    val focused by source.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .tmrPressScale(source)
            .tmrFocusRing(focused, TmrTheme.colors.primary, 24.dp)
            .defaultMinSize(minHeight = minHeight),
        shape = TmrTheme.shapes.pill,
        color = palette.container,
        contentColor = palette.content,
        border = palette.border,
        interactionSource = source,
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.content) {
            ProvideTextStyle(TmrTheme.typography.button) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = minHeight)
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun TmrButtonLabeled(
    kind: TmrButtonKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    size: TmrButtonSize = TmrButtonSize.Large,
    labelStyle: TextStyle? = null,
) {
    TmrButtonBase(
        kind = kind,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        contentPadding = when {
            kind == TmrButtonKind.Text -> TmrButtonDefaults.TextContentPadding
            size == TmrButtonSize.Compact -> TmrButtonDefaults.CompactContentPadding
            else -> TmrButtonDefaults.ContentPadding
        },
    ) {
        TmrButtonIcon(leadingIcon, size)
        var wrapped by remember { mutableStateOf(false) }
        Text(
            text = label,
            style = labelStyle ?: if (size == TmrButtonSize.Compact) TmrTheme.typography.labelL else TmrTheme.typography.button,
            textAlign = if (wrapped) TextAlign.Center else TextAlign.Unspecified,
            onTextLayout = { wrapped = it.lineCount > 1 },
        )
        TmrButtonIcon(trailingIcon, size)
    }
}

@Composable
private fun TmrButtonIcon(icon: ImageVector?, size: TmrButtonSize) {
    if (icon != null) {
        val iconSize = if (size == TmrButtonSize.Compact) TmrButtonDefaults.CompactIconSize else TmrButtonDefaults.IconSize
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun TmrButtonSlots(
    kind: TmrButtonKind,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    text: @Composable () -> Unit,
    leadingIcon: @Composable (() -> Unit)?,
) {
    TmrButtonBase(
        kind = kind,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = TmrButtonDefaults.ContentPadding,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        text()
    }
}

@Composable
fun TmrButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = TmrButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    TmrButtonBase(TmrButtonKind.Primary, onClick, modifier, enabled, contentPadding, content = content)
}

@Composable
fun TmrButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    TmrButtonSlots(TmrButtonKind.Primary, onClick, modifier, enabled, text, leadingIcon)
}

@Composable
fun TmrOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = TmrButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    TmrButtonBase(TmrButtonKind.Outline, onClick, modifier, enabled, contentPadding, content = content)
}

@Composable
fun TmrOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    TmrButtonSlots(TmrButtonKind.Outline, onClick, modifier, enabled, text, leadingIcon)
}

@Composable
fun TmrPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    size: TmrButtonSize = TmrButtonSize.Large,
) {
    TmrButtonLabeled(TmrButtonKind.Primary, label, onClick, modifier, enabled, leadingIcon, trailingIcon, size)
}

@Composable
fun TmrSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    size: TmrButtonSize = TmrButtonSize.Large,
) {
    TmrButtonLabeled(TmrButtonKind.Secondary, label, onClick, modifier, enabled, leadingIcon, trailingIcon, size)
}

@Composable
fun TmrOutlineButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    size: TmrButtonSize = TmrButtonSize.Large,
    labelStyle: TextStyle? = null,
) {
    TmrButtonLabeled(TmrButtonKind.Outline, label, onClick, modifier, enabled, leadingIcon, trailingIcon, size, labelStyle)
}

@Composable
fun TmrTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    TmrButtonLabeled(TmrButtonKind.Text, label, onClick, modifier, enabled, leadingIcon, trailingIcon)
}

@Composable
fun TmrInkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    TmrButtonLabeled(TmrButtonKind.Ink, label, onClick, modifier, enabled, leadingIcon, trailingIcon)
}

@Composable
fun TmrDestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    size: TmrButtonSize = TmrButtonSize.Large,
    filled: Boolean = true,
) {
    val kind = if (filled) TmrButtonKind.Destructive else TmrButtonKind.DestructiveText
    TmrButtonLabeled(kind, label, onClick, modifier, enabled, leadingIcon, null, size)
}
