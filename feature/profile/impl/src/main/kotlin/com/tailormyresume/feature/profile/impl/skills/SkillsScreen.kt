package com.tailormyresume.feature.profile.impl.skills

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
) = Unit
