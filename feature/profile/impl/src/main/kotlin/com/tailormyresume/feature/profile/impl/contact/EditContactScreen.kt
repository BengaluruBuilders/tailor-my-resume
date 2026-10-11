package com.tailormyresume.feature.profile.impl.contact

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal data class EditContactActions(
    val onFullNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onCityChange: (String) -> Unit = {},
    val onLinkedinChange: (String) -> Unit = {},
    val onPortfolioChange: (String) -> Unit = {},
)

@Composable
internal fun EditContactScreen(
    state: EditContactUiState,
    actions: EditContactActions,
    modifier: Modifier = Modifier,
) = Unit
