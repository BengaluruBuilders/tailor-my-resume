package com.tailormyresume.feature.profile.impl.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.RegisterChromeAction
import com.tailormyresume.feature.profile.impl.R

private const val STACKED_ADD_FONT_SCALE = 1.3f

private val ADD_BUTTON_WIDTH = 96.dp

internal data class SkillsActions(
    val onNewSkillChange: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onRemove: (String) -> Unit = {},
)

@Composable
internal fun SkillsScreen(
    state: SkillsUiState,
    actions: SkillsActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SkillsUiState.Editing -> SkillsForm(state, actions, modifier)
        SkillsUiState.Loading -> Box(modifier = modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsForm(
    state: SkillsUiState.Editing,
    actions: SkillsActions,
    modifier: Modifier = Modifier,
) {
    val spacing = TmrTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.feature_profile_impl_skills_heading),
            style = TmrTheme.typography.headline,
            color = TmrTheme.colors.text,
        )
        Text(
            text = pluralStringResource(R.plurals.feature_profile_impl_skills_count, state.skills.size, state.skills.size),
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.textSecondary,
        )
        AddSkillRow(state = state, actions = actions)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            state.skills.forEach { skill ->
                SkillChip(skill = skill, onRemove = { actions.onRemove(skill) })
            }
        }
    }
}

@Composable
private fun AddSkillRow(state: SkillsUiState.Editing, actions: SkillsActions) {
    val gap = Arrangement.spacedBy(TmrTheme.spacing.sm)
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_skills_add_hint),
            value = state.newSkill,
            onValueChange = actions.onNewSkillChange,
            modifier = fieldModifier,
        )
    }
    val button: @Composable (Modifier) -> Unit = { buttonModifier ->
        AddSkillButton(
            label = stringResource(R.string.feature_profile_impl_skills_add),
            onClick = actions.onAdd,
            enabled = state.canAdd,
            modifier = buttonModifier,
        )
    }
    if (LocalDensity.current.fontScale > STACKED_ADD_FONT_SCALE) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = gap) {
            field(Modifier.fillMaxWidth())
            button(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = gap, verticalAlignment = Alignment.Bottom) {
            field(Modifier.weight(1f))
            button(Modifier.width(ADD_BUTTON_WIDTH))
        }
    }
}

@Composable
private fun AddSkillButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.pill
    Box(
        modifier = modifier
            .heightIn(min = TmrTheme.spacing.primaryButtonHeight)
            .clip(shape)
            .background(if (enabled) colors.lime else colors.disabledFill, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = TmrTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TmrTheme.typography.button,
            color = if (enabled) colors.ink else colors.textDisabled,
        )
    }
}

@Composable
private fun SkillChip(skill: String, onRemove: () -> Unit) {
    val colors = TmrTheme.colors
    val removeDescription = stringResource(R.string.feature_profile_impl_skills_remove, skill)
    Row(
        modifier = Modifier
            .clip(TmrTheme.shapes.pill)
            .background(colors.fill, TmrTheme.shapes.pill)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = skill,
            style = TmrTheme.typography.body,
            color = colors.text,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(
            modifier = Modifier
                .size(TmrTheme.spacing.d48)
                .clickable(role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.feature_profile_impl_skills_remove_mark),
                style = TmrTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
internal fun SkillsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SkillsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(R.string.feature_profile_impl_skills_saved)
    val toast = LocalTmrToast.current
    RegisterChromeAction(
        onAction = { viewModel.save() },
        enabled = state is SkillsUiState.Editing,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            navigator.goBack()
            toast.show(savedMessage)
        }
    }
    SkillsScreen(
        state = state,
        actions = SkillsActions(
            onNewSkillChange = viewModel::onNewSkillChange,
            onAdd = viewModel::onAdd,
            onRemove = viewModel::onRemove,
        ),
        modifier = modifier,
    )
}
