package com.tailormyresume.feature.onboarding.impl.manual

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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.TmrBottomActionBar
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrReadOnlyField
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.impl.R

@Composable
internal fun ManualProfileScreen(
    state: ManualProfileUiState,
    onFieldChange: (ManualField, String) -> Unit,
    onContinueClick: () -> Unit,
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
                .padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_onboarding_impl_manual_title),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TmrTheme.typography.headline,
                color = TmrTheme.colors.text,
            )
            Text(
                text = stringResource(R.string.feature_onboarding_impl_manual_intro),
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textSecondary,
            )
            TmrTextField(
                value = state.fullName,
                onValueChange = { onFieldChange(ManualField.FullName, it) },
                label = stringResource(R.string.feature_onboarding_impl_manual_full_name),
            )
            if (state.emailEditable) {
                TmrTextField(
                    value = state.email,
                    onValueChange = { onFieldChange(ManualField.Email, it) },
                    label = stringResource(R.string.feature_onboarding_impl_manual_email),
                )
            } else {
                TmrReadOnlyField(value = state.email, label = stringResource(R.string.feature_onboarding_impl_manual_email))
            }
            TmrTextField(
                value = state.phone,
                onValueChange = { onFieldChange(ManualField.Phone, it) },
                label = stringResource(R.string.feature_onboarding_impl_manual_phone),
                placeholder = stringResource(R.string.feature_onboarding_impl_manual_phone_hint),
            )
            TmrTextField(
                value = state.city,
                onValueChange = { onFieldChange(ManualField.City, it) },
                label = stringResource(R.string.feature_onboarding_impl_manual_city),
                placeholder = stringResource(R.string.feature_onboarding_impl_manual_city_hint),
            )
            TmrTextField(
                value = state.jobTitle,
                onValueChange = { onFieldChange(ManualField.JobTitle, it) },
                label = stringResource(R.string.feature_onboarding_impl_manual_job_title),
                placeholder = stringResource(R.string.feature_onboarding_impl_manual_job_title_hint),
            )
            TmrTextField(
                value = state.company,
                onValueChange = { onFieldChange(ManualField.Company, it) },
                label = stringResource(R.string.feature_onboarding_impl_manual_company),
                placeholder = stringResource(R.string.feature_onboarding_impl_manual_company_hint),
            )
        }
        TmrBottomActionBar {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_onboarding_impl_manual_continue),
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun ManualProfileRoute(
    viewModel: ManualProfileViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onRouteActive()
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { navigator.navigate(ReviewProfileNavKey()) }
    }
    ManualProfileScreen(
        state = state,
        onFieldChange = viewModel::onFieldChange,
        onContinueClick = viewModel::onContinue,
        modifier = modifier,
    )
}
