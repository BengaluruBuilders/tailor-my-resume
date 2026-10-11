package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.settings.impl.R

private val MarkSize = 56.dp

@Composable
internal fun ColumnScope.PaymentFailedSheetContent(
    onTryAgain: () -> Unit,
    onCancel: () -> Unit,
    unconfirmed: Boolean = false,
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(MarkSize).background(colors.cheek, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.feature_settings_impl_pay_failed_mark),
                style = typography.display.copy(fontSize = 26.sp, lineHeight = 26.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
        }
        Text(
            text = stringResource(R.string.feature_settings_impl_pay_failed_title_broken),
            style = typography.title,
            color = colors.text,
        )
        Text(
            text = stringResource(
                if (unconfirmed) R.string.feature_settings_impl_pay_unconfirmed_body else R.string.feature_settings_impl_pay_failed_body,
            ),
            style = typography.bodySmall,
            color = colors.textSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_settings_impl_pay_failed_retry),
                onClick = onTryAgain,
                modifier = Modifier.fillMaxWidth(),
            )
            TmrSecondaryButton(
                label = stringResource(R.string.feature_settings_impl_pay_failed_cancel),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
