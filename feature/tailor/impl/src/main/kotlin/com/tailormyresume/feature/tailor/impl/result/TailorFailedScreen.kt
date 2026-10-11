package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.tailor.impl.R

@Composable
internal fun TailorFailedScreen(onTryAgain: () -> Unit, onGoBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TmrTheme.colors
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(TmrTheme.shapes.hero)
                    .background(colors.cheek, TmrTheme.shapes.hero),
                contentAlignment = Alignment.BottomCenter,
            ) {
                TmrPaige(TmrPaigePose.Fail, Modifier.offset(y = 36.dp))
            }
            Text(
                text = stringResource(R.string.feature_tailor_impl_result_failed_title),
                style = TmrTheme.typography.headline,
                color = colors.text,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                text = stringResource(R.string.feature_tailor_impl_result_failed_body),
                style = TmrTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_tailor_impl_result_failed_retry),
                onClick = onTryAgain,
                modifier = Modifier.fillMaxWidth(),
            )
            TmrSecondaryButton(
                label = stringResource(R.string.feature_tailor_impl_result_failed_back),
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
