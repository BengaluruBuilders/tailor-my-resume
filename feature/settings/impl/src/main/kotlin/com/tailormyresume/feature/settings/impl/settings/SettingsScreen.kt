package com.tailormyresume.feature.settings.impl.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrDivider
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrListRow
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.component.input.TmrToggle
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.settings.impl.R

internal class SettingsActions(
    val onCredits: () -> Unit,
    val onPageSize: () -> Unit,
    val onFileName: () -> Unit,
    val onProductUpdates: () -> Unit,
    val onDownloadData: () -> Unit,
    val onHelp: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onSignOut: () -> Unit,
)

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    versionName: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val content = state as? SettingsUiState.Content ?: return
    val colors = TmrTheme.colors
    val spacing = TmrTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.feature_settings_impl_title),
            style = TmrTheme.typography.headlineSmall,
            color = colors.text,
        )
        TmrSectionLabel(stringResource(R.string.feature_settings_impl_section_account))
        SettingsGroup {
            AccountIdentity(email = content.email)
            SettingsValueRow(
                label = stringResource(R.string.feature_settings_impl_credits_row),
                value = pluralStringResource(R.plurals.feature_settings_impl_credits_left, content.credits, content.credits),
                onClick = actions.onCredits,
                showChevron = true,
                showDivider = false,
            )
        }
        TmrSectionLabel(stringResource(R.string.feature_settings_impl_section_resume))
        SettingsGroup {
            SettingsValueRow(
                label = stringResource(R.string.feature_settings_impl_page_size),
                value = stringResource(content.pageSize.labelRes()),
                onClick = actions.onPageSize,
                showChevron = false,
                showDivider = true,
            )
            SettingsValueRow(
                label = stringResource(R.string.feature_settings_impl_file_name),
                value = stringResource(content.fileNameFormat.labelRes()),
                onClick = actions.onFileName,
                showChevron = false,
                showDivider = false,
            )
        }
        TmrSectionLabel(stringResource(R.string.feature_settings_impl_section_notifications))
        SettingsGroup {
            TmrToggle(
                label = stringResource(R.string.feature_settings_impl_product_updates),
                checked = content.productUpdates,
                onCheckedChange = { actions.onProductUpdates() },
            )
        }
        TmrSectionLabel(stringResource(R.string.feature_settings_impl_section_privacy))
        SettingsGroup {
            TmrListRow(
                label = stringResource(R.string.feature_settings_impl_download_data),
                onClick = actions.onDownloadData,
            )
            TmrListRow(
                label = stringResource(R.string.feature_settings_impl_help),
                onClick = actions.onHelp,
            )
            DestructiveRow(
                label = stringResource(R.string.feature_settings_impl_delete_account),
                onClick = actions.onDeleteAccount,
            )
        }
        Spacer(Modifier.height(spacing.xs))
        TmrSecondaryButton(
            label = stringResource(R.string.feature_settings_impl_sign_out),
            onClick = actions.onSignOut,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.feature_settings_impl_footer, versionName),
            style = TmrTheme.typography.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xs, bottom = spacing.xxxl),
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    TmrCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AccountIdentity(email: String) {
    val colors = TmrTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(vertical = TmrTheme.spacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.xxs, Alignment.CenterVertically),
    ) {
        Text(text = email, style = TmrTheme.typography.body, color = colors.text)
        Text(
            text = stringResource(R.string.feature_settings_impl_signed_in_with_google),
            style = TmrTheme.typography.caption,
            color = colors.textMuted,
        )
    }
    TmrDivider(color = colors.line)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showChevron: Boolean,
    showDivider: Boolean,
) {
    val colors = TmrTheme.colors
    Column(Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = TmrTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = TmrTheme.typography.body,
                color = colors.text,
                modifier = Modifier.padding(end = TmrTheme.spacing.md),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, style = TmrTheme.typography.caption, color = colors.textMuted)
                if (showChevron) {
                    Icon(
                        imageVector = TmrIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (showDivider) TmrDivider(color = colors.line)
    }
}

@Composable
private fun DestructiveRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = TmrTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = TmrTheme.typography.body, color = TmrTheme.colors.cheek)
    }
}

private fun PageSize.labelRes(): Int = when (this) {
    PageSize.A4 -> R.string.feature_settings_impl_page_a4
    PageSize.LETTER -> R.string.feature_settings_impl_page_letter
}

private fun FileNameFormat.labelRes(): Int = when (this) {
    FileNameFormat.NAME_COMPANY_ROLE -> R.string.feature_settings_impl_file_name_company_role
    FileNameFormat.NAME_ROLE -> R.string.feature_settings_impl_file_name_role
    FileNameFormat.NAME_RESUME -> R.string.feature_settings_impl_file_name_resume
}
