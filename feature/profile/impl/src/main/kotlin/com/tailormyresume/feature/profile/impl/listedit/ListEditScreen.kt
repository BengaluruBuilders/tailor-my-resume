package com.tailormyresume.feature.profile.impl.listedit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal data class ListEditActions(
    val onTextChange: (Int, String) -> Unit = { _, _ -> },
    val onAdd: () -> Unit = {},
)

@Composable
internal fun ListEditScreen(
    state: ListEditUiState,
    actions: ListEditActions,
    modifier: Modifier = Modifier,
) = Unit
