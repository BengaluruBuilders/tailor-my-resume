package com.tailormyresume.feature.settings.impl.referrals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrReadOnlyField
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.component.input.TmrTextButton
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.ReferralMessage
import com.tailormyresume.core.domain.ReferralUiState
import com.tailormyresume.feature.settings.impl.R
import kotlinx.coroutines.launch

internal class ReferralActions(
    val refresh: () -> Unit,
    val issue: () -> Unit,
    val save: (String, Boolean) -> Unit,
    val clear: () -> Unit,
    val apply: () -> Unit,
    val review: () -> Unit,
    val google: () -> Unit,
    val copy: (String) -> Unit,
    val share: () -> Unit = {},
)

@Composable
internal fun ReferralsRoute(modifier: Modifier = Modifier, viewModel: ReferralsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val content = state as? ReferralUiState.Content
    val request by viewModel.routeRequests.collectAsStateWithLifecycle()
    LaunchedEffect(content?.candidate, content?.offerVersion, request) {
        content?.candidate?.let { code ->
            androidx.compose.runtime.withFrameNanos { }
            viewModel.displayed(code, content.offerVersion)
        }
    }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel, lifecycle) {
        viewModel.refresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val context = LocalContext.current
    val toast = LocalTmrToast.current
    val copied = stringResource(R.string.feature_settings_impl_referral_copied)
    val label = stringResource(R.string.feature_settings_impl_referral_my_code)
    val actions = remember(viewModel, context, toast, copied, label) {
        ReferralActions(
            viewModel::refresh, viewModel::issue, viewModel::save, viewModel::clear, viewModel::apply, viewModel::review, viewModel::google,
            copy = { code ->
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(label, code))
                toast.show(copied)
            },
            share = {
                scope.launch {
                    viewModel.share()?.let { value ->
                        val destination = value.url ?: value.code
                        val text = if (value.reward != null && value.version != null) {
                            context.getString(R.string.feature_settings_impl_referral_share_offer, value.reward, value.version, destination)
                        } else {
                            context.getString(R.string.feature_settings_impl_referral_share_neutral, destination)
                        }
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
                        context.startActivity(android.content.Intent.createChooser(send, context.getString(R.string.feature_settings_impl_referral_share)))
                    }
                }
            },
        )
    }
    ReferralsScreen(state, actions, modifier)
}

@Composable
internal fun ReferralsScreen(state: ReferralUiState, actions: ReferralActions, modifier: Modifier = Modifier) {
    val content = state as? ReferralUiState.Content
    val spacing = TmrTheme.spacing
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = spacing.gutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(stringResource(R.string.feature_settings_impl_referral_title), style = TmrTheme.typography.headlineSmall, color = TmrTheme.colors.text)
        if (content == null) {
            Text(stringResource(R.string.feature_settings_impl_referral_loading), style = TmrTheme.typography.body, color = TmrTheme.colors.textSecondary)
            return@Column
        }
        val reward = content.reward
        Text(
            if (reward != null) pluralStringResource(R.plurals.feature_settings_impl_referral_offer, reward, reward) else stringResource(R.string.feature_settings_impl_referral_unavailable),
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.textSecondary,
        )
        content.offerVersion?.takeIf { content.reward != null }?.let { version ->
            Text(stringResource(R.string.feature_settings_impl_referral_offer_version, version), style = TmrTheme.typography.caption, color = TmrTheme.colors.textMuted)
        }
        val personalCode = content.code
        if (personalCode != null) {
            TmrReadOnlyField(personalCode, stringResource(R.string.feature_settings_impl_referral_my_code))
            TmrSecondaryButton(stringResource(R.string.feature_settings_impl_referral_copy), { actions.copy(personalCode) }, Modifier.fillMaxWidth(), enabled = !content.busy)
            if (content.shareUrl != null) TmrSecondaryButton(stringResource(R.string.feature_settings_impl_referral_share), actions.share, Modifier.fillMaxWidth(), enabled = !content.busy)
        } else if (content.canIssue) {
            TmrSecondaryButton(stringResource(R.string.feature_settings_impl_referral_create), actions.issue, Modifier.fillMaxWidth(), enabled = !content.busy)
        }
        Text(
            stringResource(
                R.string.feature_settings_impl_referral_stats,
                pluralStringResource(R.plurals.feature_settings_impl_referral_friends, if (content.creditedCount == 1L) 1 else 2, content.creditedCount),
                pluralStringResource(R.plurals.feature_settings_impl_referral_earned, if (content.earnedCredits == 1L) 1 else 2, content.earnedCredits),
            ),
            style = TmrTheme.typography.caption,
            color = TmrTheme.colors.textMuted,
        )
        if (content.attached) {
            Text(stringResource(R.string.feature_settings_impl_referral_applied), style = TmrTheme.typography.body, color = TmrTheme.colors.lime)
        } else if (content.message != ReferralMessage.ReadOnly) {
            content.deadline?.let { deadline ->
                val formatted = remember(deadline) { java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", java.util.Locale.ENGLISH).withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.parse(deadline)) }
                Text(stringResource(R.string.feature_settings_impl_referral_deadline, formatted), style = TmrTheme.typography.caption, color = TmrTheme.colors.textSecondary)
            }
            ReferralEntry(content, actions)
        }
        if (content.message != ReferralMessage.None && content.message != ReferralMessage.Applied) {
            Text(stringResource(content.message.text()), style = TmrTheme.typography.body, color = TmrTheme.colors.textSecondary)
        }
        content.retryAfterSeconds?.let { seconds ->
            Text(pluralStringResource(R.plurals.feature_settings_impl_referral_wait, seconds, seconds), style = TmrTheme.typography.caption, color = TmrTheme.colors.textSecondary)
        }
        if (content.guest) TmrSecondaryButton(stringResource(R.string.feature_settings_impl_google_account), actions.google, Modifier.fillMaxWidth(), enabled = !content.busy)
        TmrTextButton(stringResource(R.string.feature_settings_impl_referral_refresh), actions.refresh, Modifier.fillMaxWidth(), enabled = !content.busy)
    }
}

@Composable
private fun ReferralEntry(content: ReferralUiState.Content, actions: ReferralActions) {
    var draft by remember(content.guest, content.reviewRequired, content.candidate) { mutableStateOf("") }
    TmrCard {
        Column(verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm)) {
            Text(stringResource(R.string.feature_settings_impl_referral_entry), style = TmrTheme.typography.bodyLarge, color = TmrTheme.colors.text)
            Text(stringResource(R.string.feature_settings_impl_referral_optional), style = TmrTheme.typography.caption, color = TmrTheme.colors.textSecondary)
            content.candidate?.let { TmrReadOnlyField(it, stringResource(R.string.feature_settings_impl_referral_saved_code)) }
            TmrTextField(
                draft,
                { if (it.length <= 256) draft = it },
                stringResource(R.string.feature_settings_impl_referral_code),
                enabled = !content.busy,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            )
            TmrSecondaryButton(
                stringResource(if (content.message == ReferralMessage.ReplaceRequired) R.string.feature_settings_impl_referral_replace else R.string.feature_settings_impl_referral_save),
                { actions.save(draft, content.message == ReferralMessage.ReplaceRequired) },
                Modifier.fillMaxWidth(),
                enabled = !content.busy && draft.isNotBlank(),
            )
            if (content.candidate != null) {
                if (content.reviewRequired) {
                    TmrSecondaryButton(stringResource(R.string.feature_settings_impl_referral_review), actions.review, Modifier.fillMaxWidth(), enabled = !content.busy)
                } else {
                    TmrPrimaryButton(
                        stringResource(
                            if (content.message == ReferralMessage.PolicyChanged) {
                                R.string.feature_settings_impl_referral_reconfirm
                            } else if (content.candidateSource == "LINK") {
                                R.string.feature_settings_impl_referral_continue
                            } else {
                                R.string.feature_settings_impl_referral_apply
                            },
                        ),
                        actions.apply,
                        Modifier.fillMaxWidth(),
                        enabled = !content.busy && content.canAttach && content.retryAfterSeconds == null,
                    )
                }
                TmrTextButton(stringResource(R.string.feature_settings_impl_referral_clear), actions.clear, Modifier.fillMaxWidth(), enabled = !content.busy && content.message != ReferralMessage.ReadOnly)
            }
        }
    }
}

private fun ReferralMessage.text(): Int = when (this) {
    ReferralMessage.None, ReferralMessage.Unavailable -> R.string.feature_settings_impl_referral_unavailable
    ReferralMessage.Saved -> R.string.feature_settings_impl_referral_saved
    ReferralMessage.Invalid -> R.string.feature_settings_impl_referral_invalid
    ReferralMessage.ReplaceRequired -> R.string.feature_settings_impl_referral_replace_prompt
    ReferralMessage.Applied -> R.string.feature_settings_impl_referral_applied
    ReferralMessage.PolicyChanged -> R.string.feature_settings_impl_referral_policy_changed
    ReferralMessage.ReadOnly -> R.string.feature_settings_impl_referral_read_only
    ReferralMessage.Ineligible -> R.string.feature_settings_impl_referral_ineligible
    ReferralMessage.Expired -> R.string.feature_settings_impl_referral_expired
    ReferralMessage.Self -> R.string.feature_settings_impl_referral_self
    ReferralMessage.Conflict, ReferralMessage.SignalChanged -> R.string.feature_settings_impl_referral_conflict
    ReferralMessage.Retry -> R.string.feature_settings_impl_referral_retry
}
