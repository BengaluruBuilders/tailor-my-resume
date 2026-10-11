package com.tailormyresume.feature.settings.impl.delete

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.settings.impl.R
import kotlinx.coroutines.launch

private val HeroHeight = 140.dp

private val PaigeDrop = 44.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DeleteAccountSheetContent(
    state: DeleteAccountUiState.Content,
    onTextChanged: (String) -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val fieldInView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .clip(TmrTheme.shapes.hero)
                .background(colors.cheek),
            contentAlignment = Alignment.BottomCenter,
        ) {
            TmrPaige(TmrPaigePose.Delete, Modifier.offset(y = PaigeDrop))
        }
        Text(
            text = stringResource(R.string.feature_settings_impl_delete_title),
            style = typography.title,
            color = colors.text,
        )
        Text(
            text = stringResource(
                R.string.feature_settings_impl_delete_body,
                pluralStringResource(R.plurals.feature_settings_impl_delete_resumes, state.applications, state.applications),
                pluralStringResource(R.plurals.feature_settings_impl_delete_credits, state.unusedCredits, state.unusedCredits),
            ),
            style = typography.bodySmall,
            color = colors.textSecondary,
        )
        TmrTextField(
            value = state.typed,
            onValueChange = onTextChanged,
            label = stringResource(R.string.feature_settings_impl_delete_confirm_label),
            placeholder = stringResource(R.string.feature_settings_impl_delete_confirm_placeholder),
            modifier = Modifier
                .bringIntoViewRequester(fieldInView)
                .onFocusEvent { focus -> if (focus.isFocused) scope.launch { fieldInView.bringIntoView() } },
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_settings_impl_delete_account),
                onClick = onDelete,
                enabled = state.canDelete && !state.deleting,
                modifier = Modifier.fillMaxWidth(),
            )
            TmrSecondaryButton(
                label = stringResource(R.string.feature_settings_impl_keep_account),
                onClick = onKeep,
                enabled = !state.deleting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun DeleteAccountSheet(
    viewModel: DeleteAccountViewModel,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalTmrToast.current
    val sheetHost = LocalTmrSheetHost.current
    val failedMessage = stringResource(R.string.feature_settings_impl_toast_delete_failed)
    DisposableEffect(viewModel) {
        viewModel.onSheetOpened()
        onDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DeleteAccountEvent.DeleteFailed -> toast.show(failedMessage)
            }
        }
    }
    DisposableEffect(viewModel) { onDispose(viewModel::onSheetClosed) }
    val deleting = (state as? DeleteAccountUiState.Content)?.deleting == true
    SideEffect { sheetHost.dismissible = !deleting }
    (state as? DeleteAccountUiState.Content)?.let { content ->
        DeleteAccountSheetContent(
            state = content,
            onTextChanged = viewModel::onTextChanged,
            onDelete = viewModel::onDelete,
            onKeep = onKeep,
            modifier = modifier,
        )
    }
}
