package com.tailormyresume.feature.onboarding.impl.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tailormyresume.core.navigation.Navigator

@Composable
internal fun ReviewScreen(
    state: ReviewUiState.Ready,
    onAddDateClick: (entryId: String) -> Unit,
    onContactClick: () -> Unit,
    onExperienceClick: () -> Unit,
    onSkillsClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit

@Composable
internal fun ReviewRoute(
    viewModel: ReviewViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) = Unit
