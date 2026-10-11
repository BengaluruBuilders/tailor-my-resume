package com.tailormyresume.feature.profile.impl.listedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.input.TmrTextArea
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.RegisterChromeAction
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import com.tailormyresume.feature.profile.impl.R
import com.tailormyresume.feature.profile.impl.experience.DashedAddButton

internal data class ListEditActions(
    val onTextChange: (Int, String) -> Unit = { _, _ -> },
    val onAdd: () -> Unit = {},
)

private data class SectionCopy(
    val heading: Int,
    val note: Int?,
    val itemLabel: Int,
    val addLabel: Int?,
)

private fun ProfileListSection.copy() = when (this) {
    ProfileListSection.SUMMARY -> SectionCopy(
        heading = R.string.feature_profile_impl_list_summary_heading,
        note = R.string.feature_profile_impl_list_summary_note,
        itemLabel = R.string.feature_profile_impl_list_summary_label,
        addLabel = null,
    )
    ProfileListSection.EDUCATION -> SectionCopy(
        heading = R.string.feature_profile_impl_list_education_heading,
        note = null,
        itemLabel = R.string.feature_profile_impl_list_item_label,
        addLabel = R.string.feature_profile_impl_list_education_add,
    )
    ProfileListSection.ACHIEVEMENTS -> SectionCopy(
        heading = R.string.feature_profile_impl_list_achievements_heading,
        note = R.string.feature_profile_impl_list_achievements_note,
        itemLabel = R.string.feature_profile_impl_list_item_label,
        addLabel = R.string.feature_profile_impl_list_achievements_add,
    )
}

@Composable
internal fun ListEditScreen(
    state: ListEditUiState,
    actions: ListEditActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ListEditUiState.Editing -> ListEditForm(state, actions, modifier)
        ListEditUiState.Loading -> Box(modifier = modifier)
    }
}

@Composable
private fun ListEditForm(
    state: ListEditUiState.Editing,
    actions: ListEditActions,
    modifier: Modifier = Modifier,
) {
    val spacing = TmrTheme.spacing
    val copy = state.section.copy()
    val hint = stringResource(R.string.feature_profile_impl_list_hint)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(copy.heading),
            style = TmrTheme.typography.headline,
            color = TmrTheme.colors.text,
        )
        copy.note?.let { note ->
            Text(
                text = stringResource(note),
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textSecondary,
            )
        }
        state.texts.forEachIndexed { index, text ->
            TmrTextArea(
                label = stringResource(copy.itemLabel, index + 1),
                value = text,
                onValueChange = { actions.onTextChange(index, it) },
                placeholder = hint,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canAdd) {
            copy.addLabel?.let { addLabel ->
                DashedAddButton(label = stringResource(addLabel), onClick = actions.onAdd)
            }
        }
    }
}

@Composable
internal fun ListEditRoute(
    navigator: Navigator,
    section: ProfileListSection,
    modifier: Modifier = Modifier,
) {
    val viewModel: ListEditViewModel = hiltViewModel<ListEditViewModel, ListEditViewModel.Factory>(
        creationCallback = { factory -> factory.create(section) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(R.string.feature_profile_impl_list_saved)
    val toast = LocalTmrToast.current
    RegisterChromeAction(
        onAction = { viewModel.save() },
        enabled = (state as? ListEditUiState.Editing)?.canSave == true,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            navigator.goBack()
            toast.show(savedMessage)
        }
    }
    ListEditScreen(
        state = state,
        actions = ListEditActions(onTextChange = viewModel::onTextChange, onAdd = viewModel::onAdd),
        modifier = modifier,
    )
}
