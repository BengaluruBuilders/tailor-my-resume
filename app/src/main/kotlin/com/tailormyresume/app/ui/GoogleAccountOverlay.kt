package com.tailormyresume.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.app.R
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.component.input.TmrToggle
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.GoogleAccountGateway
import com.tailormyresume.core.domain.GoogleAccountState
import kotlinx.coroutines.launch

@Composable
internal fun GoogleAccountOverlay(gateway: GoogleAccountGateway, modifier: Modifier = Modifier) {
    val state by gateway.state.collectAsStateWithLifecycle()
    if (state == GoogleAccountState.Idle) return
    val scope = rememberCoroutineScope()
    BackHandler {
        if (state !is GoogleAccountState.Busy && (state as? GoogleAccountState.Retry)?.canCancel != false) {
            scope.launch { gateway.dismiss() }
        }
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        GoogleAccountPanel(
            state,
            onClaim = { scope.launch { gateway.claim() } },
            onRetry = { scope.launch { gateway.retry() } },
            onCancel = { scope.launch { gateway.cancel() } },
            onClose = { scope.launch { gateway.dismiss() } },
            onConflict = { profile, apps -> scope.launch { gateway.continueConflict(profile, apps) } },
            onProfileChoice = { useGuest -> scope.launch { gateway.chooseProfile(useGuest) } },
            modifier = modifier,
        )
    }

}

@Composable
internal fun GoogleAccountPanel(
    state: GoogleAccountState,
    onClaim: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onConflict: (Boolean, Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    onProfileChoice: (Boolean) -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().background(TmrTheme.colors.background).testTag("google-account-panel")
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.google_account_title), style = TmrTheme.typography.headline, color = TmrTheme.colors.text)
        when (state) {
            GoogleAccountState.Busy -> Body(stringResource(R.string.google_account_working))
            GoogleAccountState.SaveEdits -> {
                Body(stringResource(R.string.google_account_save_edits))
                TmrPrimaryButton(stringResource(R.string.google_account_return), onClose, Modifier.fillMaxWidth())
            }
            is GoogleAccountState.Offer -> {
                Body(stringResource(if (state.google) R.string.google_account_offer_intro else R.string.google_account_guest_intro))
                state.signup?.let { Body(pluralStringResource(R.plurals.google_account_signup_offer, it, it)) }
                state.base?.let { Body(pluralStringResource(R.plurals.google_account_base_offer, it, it)) }
                if (state.signup == null && state.base == null) Body(stringResource(R.string.google_account_neutral))
                state.retryAfterSeconds?.let { Body(pluralStringResource(R.plurals.google_account_retry_delay, it, it)) }
                if (state.changed) Body(stringResource(R.string.google_account_offer_changed))
                if (state.canClaim || !state.google) {
                    TmrPrimaryButton(stringResource(if (state.google) R.string.google_account_claim else R.string.google_account_continue_google), onClaim, Modifier.fillMaxWidth())
                }
                TmrSecondaryButton(stringResource(R.string.google_account_check_again), onRetry, Modifier.fillMaxWidth())
                TmrSecondaryButton(stringResource(R.string.google_account_close), onClose, Modifier.fillMaxWidth())
            }
            is GoogleAccountState.Conflict -> ConflictChoices(state, onConflict, onCancel)
            is GoogleAccountState.ProfileChoice -> {
                Body(stringResource(R.string.google_account_profile_collision, state.guestName, state.destinationName))
                TmrPrimaryButton(stringResource(R.string.google_account_use_guest_profile), { onProfileChoice(true) }, Modifier.fillMaxWidth())
                TmrSecondaryButton(stringResource(R.string.google_account_use_existing_profile), { onProfileChoice(false) }, Modifier.fillMaxWidth())
            }
            is GoogleAccountState.Retry -> {
                Body(stringResource(R.string.google_account_retry_body))
                state.retryAfterSeconds?.let { Body(pluralStringResource(R.plurals.google_account_retry_delay, it, it)) }
                TmrPrimaryButton(stringResource(R.string.google_account_retry), onRetry, Modifier.fillMaxWidth())
                if (state.canCancel) TmrSecondaryButton(stringResource(R.string.google_account_close), onClose, Modifier.fillMaxWidth())
            }
            is GoogleAccountState.Complete -> {
                Body(stringResource(if (state.preservedWork) R.string.google_account_work_preserved else R.string.google_account_ready))
                state.signup?.let { Body(pluralStringResource(R.plurals.google_account_signup_received, it, it)) }
                state.base?.let { Body(pluralStringResource(R.plurals.google_account_base_received, it, it)) }
                TmrPrimaryButton(stringResource(R.string.google_account_close), onClose, Modifier.fillMaxWidth())
                state.retryAfterSeconds?.let { Body(pluralStringResource(R.plurals.google_account_retry_delay, it, it)) }
                if (!state.preservedWork) TmrSecondaryButton(stringResource(R.string.google_account_check_remaining), onRetry, Modifier.fillMaxWidth())
            }
            GoogleAccountState.Idle -> Unit
        }
    }
}

@Composable
private fun ConflictChoices(state: GoogleAccountState.Conflict, onContinue: (Boolean, Set<String>) -> Unit, onCancel: () -> Unit) {
    var profile by remember(state) { mutableStateOf(state.hasProfile) }
    var selected by remember(state) { mutableStateOf(state.applications.map { it.first }.toSet()) }
    Body(stringResource(R.string.google_account_conflict_body))
    Body(stringResource(R.string.google_account_conflict_credits))
    if (state.hasProfile) {
        TmrToggle(stringResource(R.string.google_account_keep_profile), profile, { profile = it }, Modifier.fillMaxWidth())
        Body(stringResource(R.string.google_account_profile_choice))
    }
    state.applications.forEach { (id, title) ->
        TmrToggle(title, id in selected, { checked -> selected = if (checked) selected + id else selected - id }, Modifier.fillMaxWidth())
    }
    TmrPrimaryButton(stringResource(R.string.google_account_existing_continue), { onContinue(profile, selected) }, Modifier.fillMaxWidth())
    TmrSecondaryButton(stringResource(R.string.google_account_keep_guest), onCancel, Modifier.fillMaxWidth())
}

@Composable
private fun Body(value: String) {
    Text(value, style = TmrTheme.typography.bodyLarge, color = TmrTheme.colors.textSecondary)
}
