package com.tailormyresume.feature.profile.impl.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.input.TmrTextArea
import com.tailormyresume.core.designsystem.component.input.TmrTextButton
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.component.input.TmrToggle
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.RegisterChromeAction
import com.tailormyresume.feature.profile.impl.R

private const val STACKED_DATES_FONT_SCALE = 1.3f

internal data class EditRoleActions(
    val onTitleChange: (String) -> Unit = {},
    val onCompanyChange: (String) -> Unit = {},
    val onStartChange: (String) -> Unit = {},
    val onEndChange: (String) -> Unit = {},
    val onCurrentChange: (Boolean) -> Unit = {},
    val onBulletChange: (Int, String) -> Unit = { _, _ -> },
    val onAddBullet: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
internal fun EditRoleScreen(
    state: EditRoleUiState,
    actions: EditRoleActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is EditRoleUiState.Editing -> EditRoleForm(state, actions, modifier)
        EditRoleUiState.Loading -> Box(modifier = modifier)
    }
}

@Composable
private fun EditRoleForm(
    state: EditRoleUiState.Editing,
    actions: EditRoleActions,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val spacing = TmrTheme.spacing
    val draft = state.draft
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(
                if (state.isNew) {
                    R.string.feature_profile_impl_role_heading_add
                } else {
                    R.string.feature_profile_impl_role_heading_edit
                },
            ),
            style = typography.headline,
            color = colors.text,
        )
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_role_job_title),
            value = draft.title,
            onValueChange = actions.onTitleChange,
            placeholder = stringResource(R.string.feature_profile_impl_role_job_title_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_role_company),
            value = draft.company,
            onValueChange = actions.onCompanyChange,
            placeholder = stringResource(R.string.feature_profile_impl_role_company_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        DateFields(
            stacked = LocalDensity.current.fontScale > STACKED_DATES_FONT_SCALE,
            start = { fieldModifier ->
                TmrTextField(
                    label = stringResource(R.string.feature_profile_impl_role_start),
                    value = draft.start,
                    onValueChange = actions.onStartChange,
                    placeholder = stringResource(R.string.feature_profile_impl_role_date_hint),
                    modifier = fieldModifier,
                )
            },
            end = { fieldModifier ->
                TmrTextField(
                    label = stringResource(R.string.feature_profile_impl_role_end),
                    value = if (draft.current) "" else draft.end,
                    onValueChange = actions.onEndChange,
                    placeholder = stringResource(
                        if (draft.current) {
                            R.string.feature_profile_impl_role_present_hint
                        } else {
                            R.string.feature_profile_impl_role_date_hint
                        },
                    ),
                    readOnly = draft.current,
                    modifier = fieldModifier.alpha(if (draft.current) 0.4f else 1f),
                )
            },
        )
        TmrToggle(
            label = stringResource(R.string.feature_profile_impl_role_current),
            checked = draft.current,
            onCheckedChange = actions.onCurrentChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.feature_profile_impl_role_what_you_did),
            style = typography.label,
            color = colors.textSecondary,
            modifier = Modifier.semantics { heading() },
        )
        draft.bullets.forEachIndexed { index, bullet ->
            TmrTextArea(
                label = stringResource(R.string.feature_profile_impl_role_bullet_label, index + 1),
                value = bullet,
                onValueChange = { actions.onBulletChange(index, it) },
                placeholder = stringResource(R.string.feature_profile_impl_role_bullet_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canAddBullet) {
            DashedAddButton(
                label = stringResource(R.string.feature_profile_impl_role_add_bullet),
                onClick = actions.onAddBullet,
            )
        }
        if (!state.isNew) {
            TmrTextButton(
                label = stringResource(R.string.feature_profile_impl_role_delete),
                onClick = actions.onDelete,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DateFields(
    stacked: Boolean,
    start: @Composable (Modifier) -> Unit,
    end: @Composable (Modifier) -> Unit,
) {
    val gap = Arrangement.spacedBy(TmrTheme.spacing.sm)
    if (stacked) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = gap) {
            start(Modifier.fillMaxWidth())
            end(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = gap) {
            start(Modifier.weight(1f))
            end(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun EditRoleRoute(
    navigator: Navigator,
    entryId: String?,
    modifier: Modifier = Modifier,
) {
    val viewModel: EditRoleViewModel = hiltViewModel<EditRoleViewModel, EditRoleViewModel.Factory>(
        creationCallback = { factory -> factory.create(entryId) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(R.string.feature_profile_impl_role_saved)
    val deletedMessage = stringResource(R.string.feature_profile_impl_role_deleted)
    val toast = LocalTmrToast.current
    RegisterChromeAction(
        onAction = { viewModel.save() },
        enabled = (state as? EditRoleUiState.Editing)?.canSave == true,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            navigator.goBack()
            toast.show(
                when (event) {
                    EditRoleEvent.Saved -> savedMessage
                    EditRoleEvent.Deleted -> deletedMessage
                },
            )
        }
    }
    EditRoleScreen(
        state = state,
        actions = EditRoleActions(
            onTitleChange = viewModel::onTitleChange,
            onCompanyChange = viewModel::onCompanyChange,
            onStartChange = viewModel::onStartChange,
            onEndChange = viewModel::onEndChange,
            onCurrentChange = viewModel::onCurrentChange,
            onBulletChange = viewModel::onBulletChange,
            onAddBullet = viewModel::onAddBullet,
            onDelete = viewModel::delete,
        ),
        modifier = modifier,
    )
}
