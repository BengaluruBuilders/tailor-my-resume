package com.tailormyresume.feature.profile.impl.contact

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.input.TmrLabeledField
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.RegisterChromeAction
import com.tailormyresume.feature.profile.impl.R

private const val ZERO_WIDTH_SPACE = '​'

private val EMAIL_BREAK_AFTER = charArrayOf('@', '.')

internal data class EditContactActions(
    val onFullNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onCityChange: (String) -> Unit = {},
    val onLinkedinChange: (String) -> Unit = {},
    val onPortfolioChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
)

@Composable
internal fun EditContactScreen(
    state: EditContactUiState,
    actions: EditContactActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is EditContactUiState.Editing -> EditContactForm(state, actions, modifier)
        EditContactUiState.Loading -> Box(modifier = modifier)
    }
}

@Composable
private fun EditContactForm(
    state: EditContactUiState.Editing,
    actions: EditContactActions,
    modifier: Modifier = Modifier,
) {
    val spacing = TmrTheme.spacing
    val draft = state.draft
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.feature_profile_impl_contact_heading),
            style = TmrTheme.typography.headline,
            color = TmrTheme.colors.text,
        )
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_contact_full_name),
            value = draft.fullName,
            onValueChange = actions.onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
        )
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_contact_phone),
            value = draft.phone,
            onValueChange = actions.onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
        )
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_contact_city),
            value = draft.city,
            onValueChange = actions.onCityChange,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.emailEditable) {
            TmrTextField(label = stringResource(R.string.feature_profile_impl_contact_email_guest), value = draft.email, onValueChange = actions.onEmailChange, modifier = Modifier.fillMaxWidth())
        } else {
            ReadOnlyEmail(email = state.email, fromGoogle = state.emailFromGoogle)
        }
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_contact_linkedin),
            value = draft.linkedin,
            onValueChange = actions.onLinkedinChange,
            placeholder = stringResource(R.string.feature_profile_impl_contact_linkedin_hint),
            amber = draft.linkedin.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        TmrTextField(
            label = stringResource(R.string.feature_profile_impl_contact_portfolio),
            value = draft.portfolio,
            onValueChange = actions.onPortfolioChange,
            placeholder = stringResource(R.string.feature_profile_impl_contact_portfolio_hint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReadOnlyEmail(email: String, fromGoogle: Boolean, modifier: Modifier = Modifier) {
    val colors = TmrTheme.colors
    TmrLabeledField(
        label = stringResource(
            if (fromGoogle) R.string.feature_profile_impl_contact_email else R.string.feature_profile_impl_contact_email_resume,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, colors.line), TmrTheme.shapes.field)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = withBreakOpportunities(email),
                style = TmrTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.semantics { text = AnnotatedString(email) },
            )
        }
    }
}

private fun withBreakOpportunities(text: String): String = buildString {
    text.forEach { char ->
        append(char)
        if (char in EMAIL_BREAK_AFTER) append(ZERO_WIDTH_SPACE)
    }
}

@Composable
internal fun EditContactRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: EditContactViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(R.string.feature_profile_impl_contact_saved)
    val toast = LocalTmrToast.current
    RegisterChromeAction(
        onAction = { viewModel.save() },
        enabled = (state as? EditContactUiState.Editing)?.canSave == true,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            navigator.goBack()
            toast.show(savedMessage)
        }
    }
    EditContactScreen(
        state = state,
        actions = EditContactActions(
            onFullNameChange = viewModel::onFullNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onCityChange = viewModel::onCityChange,
            onLinkedinChange = viewModel::onLinkedinChange,
            onPortfolioChange = viewModel::onPortfolioChange,
            onEmailChange = viewModel::onEmailChange,
        ),
        modifier = modifier,
    )
}
