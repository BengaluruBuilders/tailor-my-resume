package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val TOP_BAR_HEIGHT = 50.dp

private val TOP_BAR_PADDING_TOP = 6.dp

private val TOP_BAR_ICON_SIZE = 18.dp

private val TOP_BAR_ACTION_PADDING = 18.dp

enum class TmrTopBarLeading { Back, Close, None }

@Composable
fun TmrTopBar(
    leading: TmrTopBarLeading,
    onLeading: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val shapes = TmrTheme.shapes
    val spacing = TmrTheme.spacing
    val leadingIcon =
        when (leading) {
            TmrTopBarLeading.Back -> TmrIcons.Back
            TmrTopBarLeading.Close -> TmrIcons.Close
            TmrTopBarLeading.None -> null
        }
    val leadingLabel =
        when (leading) {
            TmrTopBarLeading.Back -> stringResource(R.string.core_designsystem_chrome_back)
            TmrTopBarLeading.Close -> stringResource(R.string.core_designsystem_chrome_close)
            TmrTopBarLeading.None -> null
        }
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = TOP_BAR_HEIGHT)
            .padding(
                top = TOP_BAR_PADDING_TOP,
                start = spacing.d16,
                end = spacing.d16,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(spacing.topBarButton),
            contentAlignment = Alignment.Center,
        ) {
            if (leadingIcon == null || leadingLabel == null) {
                Spacer(modifier = Modifier.size(spacing.topBarButton))
            } else {
                Box(
                    modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clip(shapes.pill)
                        .clickable(role = Role.Button, onClick = onLeading)
                        .testTag(TmrChromeTags.TOP_BAR_LEADING)
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = leadingLabel
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .size(spacing.topBarButton)
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(TOP_BAR_ICON_SIZE),
                            tint = colors.text,
                        )
                    }
                }
            }
        }
        Box(
            modifier =
            Modifier
                .weight(1f)
                .padding(horizontal = spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            if (title != null) {
                TmrCapsText(
                    text = title,
                    style = typography.label,
                    color = colors.textMuted,
                )
            }
        }
        Box(
            modifier = Modifier.widthIn(min = spacing.topBarButton),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (action != null) {
                Box(
                    modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(TmrChromeTags.TOP_BAR_ACTION)
                        .clickable(role = Role.Button, onClick = onAction),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .heightIn(min = spacing.topBarButton)
                            .clip(shapes.pill)
                            .background(colors.lime)
                            .padding(horizontal = TOP_BAR_ACTION_PADDING),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = action,
                            style = typography.button,
                            color = colors.ink,
                        )
                    }
                }
            }
        }
    }
}
