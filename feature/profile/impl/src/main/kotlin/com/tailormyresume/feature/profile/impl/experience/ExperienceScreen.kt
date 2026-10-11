package com.tailormyresume.feature.profile.impl.experience

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.profile.api.navigation.EditRoleNavKey
import com.tailormyresume.feature.profile.impl.R

@Composable
internal fun ExperienceScreen(
    state: ExperienceUiState,
    onRole: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val spacing = TmrTheme.spacing
    val content = state as? ExperienceUiState.Content
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "heading") {
            Text(
                text = stringResource(R.string.feature_profile_impl_experience_heading),
                style = typography.headline,
                color = colors.text,
            )
        }
        if (content != null) {
            items(items = content.rows, key = { it.entryId }) { row ->
                TmrCard(
                    modifier = Modifier.clickable(role = Role.Button) { onRole(row.entryId) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            Text(
                                text = row.title,
                                style = typography.strongLarge,
                                color = colors.text,
                            )
                            Text(
                                text = row.company,
                                style = typography.caption,
                                color = colors.textSecondary,
                            )
                            val endLabel = when {
                                row.isCurrent -> stringResource(R.string.feature_profile_impl_dates_now)
                                row.needsEndDate -> stringResource(R.string.feature_profile_impl_end_date_missing)
                                else -> row.end
                            }
                            Text(
                                text = stringResource(R.string.feature_profile_impl_dates, row.start, endLabel),
                                style = typography.caption,
                                color = if (row.needsEndDate) colors.amber else colors.textMuted,
                            )
                        }
                        Icon(
                            imageVector = TmrIcons.ChevronRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            item(key = "add") {
                DashedAddButton(
                    label = stringResource(R.string.feature_profile_impl_add_role),
                    onClick = onAdd,
                )
            }
        }
    }
}

@Composable
internal fun DashedAddButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val spacing = TmrTheme.spacing
    val lineStrongColor = colors.lineStrong
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(TmrTheme.shapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = lineStrongColor,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                    ),
                )
            }
            .padding(horizontal = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.button,
            color = colors.textSecondary,
        )
    }
}

@Composable
internal fun ExperienceRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ExperienceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExperienceScreen(
        state = state,
        onRole = { id -> navigator.navigate(EditRoleNavKey(id)) },
        onAdd = { navigator.navigate(EditRoleNavKey(null)) },
        modifier = modifier,
    )
}
