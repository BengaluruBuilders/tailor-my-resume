package com.tailormyresume.feature.analysis.impl.question

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.impl.R

@Composable
internal fun QuickQuestionRoute(
    navigator: Navigator,
    viewModel: QuickQuestionViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigation = remember(navigator) { QuickQuestionNavigation(navigator) }
    val toast = LocalTmrToast.current
    val pickAnswer = LocalResources.current.getString(R.string.feature_analysis_impl_question_pick_answer)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                QuickQuestionEvent.ToastPickAnswer -> toast.show(pickAnswer)
                is QuickQuestionEvent.Tailor -> navigation.forwardToTailoring(event.applicationId)
            }
        }
    }
    when (val current = state) {
        is QuickQuestionUiState.Ready -> QuickQuestionScreen(
            state = current,
            onPick = viewModel::onPick,
            onDetailChange = viewModel::onDetailChange,
            onContinue = viewModel::onContinue,
            onSkip = viewModel::onSkip,
            modifier = modifier,
        )

        QuickQuestionUiState.NoQuestion -> {
            LaunchedEffect(viewModel) { navigation.forwardToTailoring(viewModel.applicationId) }
            Box(modifier = modifier.fillMaxSize())
        }

        QuickQuestionUiState.Loading -> Box(modifier = modifier.fillMaxSize())
    }
}
