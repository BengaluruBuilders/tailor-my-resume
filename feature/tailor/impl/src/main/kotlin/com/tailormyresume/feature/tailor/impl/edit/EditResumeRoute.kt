package com.tailormyresume.feature.tailor.impl.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.navigation.RegisterChromeAction
import com.tailormyresume.feature.tailor.impl.R

@Composable
internal fun EditResumeRoute(
    viewModel: EditResumeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = LocalTmrToast.current
    val savedMessage = stringResource(R.string.feature_tailor_impl_edit_saved)
    RegisterChromeAction(onAction = viewModel::onSave)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditResumeEvent.Saved -> {
                    toast.show(savedMessage)
                    onBack()
                }
            }
        }
    }
    (state as? EditResumeUiState.Ready)?.let { ready ->
        EditResumeScreen(state = ready, onBulletChange = viewModel::onBulletChange)
    }
}
