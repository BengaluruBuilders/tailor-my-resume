package com.tailormyresume.feature.analysis.impl.joblink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrTextField
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.analysis.impl.R

@Composable
internal fun JobLinkScreen(
    state: JobLinkUiState,
    onLinkChange: (String) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val titleStyle = TmrTheme.typography.headline
            Text(
                text = stringResource(R.string.feature_analysis_impl_joblink_title),
                style = titleStyle.copy(
                    fontSize = (titleStyle.fontSize.value * minOf(fontScale, 1.4f) / fontScale).sp,
                ),
                color = TmrTheme.colors.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = stringResource(R.string.feature_analysis_impl_joblink_body),
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
            TmrTextField(
                value = state.link,
                onValueChange = onLinkChange,
                label = stringResource(R.string.feature_analysis_impl_joblink_field_label),
                placeholder = stringResource(R.string.feature_analysis_impl_joblink_placeholder),
                modifier = Modifier.fillMaxWidth(),
                readOnly = state.importing,
            )
            Text(
                text = stringResource(R.string.feature_analysis_impl_joblink_login_hint),
                style = TmrTheme.typography.caption,
                color = TmrTheme.colors.textMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_analysis_impl_joblink_import),
                onClick = onImport,
                enabled = !state.importing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
