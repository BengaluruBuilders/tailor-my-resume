package com.tailormyresume.feature.onboarding.impl.paste

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.TmrBottomActionBar
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrTextArea
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.impl.R

@Composable
internal fun PasteResumeScreen(
    state: PasteResumeUiState,
    onTextChange: (String) -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .imePadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_onboarding_impl_paste_title),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TmrTheme.typography.headline,
                color = TmrTheme.colors.text,
            )
            Text(
                text = stringResource(R.string.feature_onboarding_impl_paste_intro),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textSecondary,
            )
            TmrTextArea(
                value = state.text,
                onValueChange = onTextChange,
                label = stringResource(R.string.feature_onboarding_impl_paste_label),
                placeholder = stringResource(R.string.feature_onboarding_impl_paste_placeholder),
            )
        }
        TmrBottomActionBar {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_onboarding_impl_paste_read),
                onClick = onReadClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canRead,
            )
        }
    }
}

@Composable
internal fun PasteResumeRoute(
    viewModel: PasteResumeViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { navigator.replace(ReadingNavKey()) }
    }
    PasteResumeScreen(
        state = state,
        onTextChange = viewModel::onTextChange,
        onReadClick = viewModel::onRead,
        modifier = modifier,
    )
}
