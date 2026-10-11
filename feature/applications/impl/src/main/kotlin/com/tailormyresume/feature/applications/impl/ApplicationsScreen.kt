package com.tailormyresume.feature.applications.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun ApplicationsScreen(
    state: ApplicationsUiState,
    onCredits: () -> Unit,
    onAvatar: () -> Unit,
    onAddJob: () -> Unit,
    onOpenApplication: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Unit
