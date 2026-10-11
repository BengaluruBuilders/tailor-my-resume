package com.tailormyresume.feature.onboarding.impl.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.TmrBottomActionBar
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.impl.R
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFileCard
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import com.tailormyresume.feature.onboarding.impl.importresume.resumeFileMeta
import com.tailormyresume.feature.onboarding.impl.importresume.resumeMimeTypes

@Composable
internal fun UnreadableScreen(
    failure: UploadFailure,
    onChooseAnotherClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background),
    ) {
        Column(
            Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(
                    if (failure.kind == UploadFailureKind.Neutral) {
                        R.string.feature_onboarding_impl_unreadable_title_neutral
                    } else {
                        R.string.feature_onboarding_impl_unreadable_title_file
                    },
                ),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TmrTheme.typography.headline,
                color = TmrTheme.colors.text,
            )
            ResumeFileCard(
                name = failure.fileName,
                meta = resumeFileMeta(
                    mimeType = failure.mimeType,
                    fileName = failure.fileName,
                    byteSize = failure.byteSize,
                    imageOnly = failure.kind == UploadFailureKind.ImageOnly,
                ),
                amber = true,
            ) {
                Text(
                    text = stringResource(
                        when (failure.kind) {
                            UploadFailureKind.ImageOnly -> R.string.feature_onboarding_impl_unreadable_explain_image_only
                            UploadFailureKind.FileProblem -> R.string.feature_onboarding_impl_unreadable_explain_file
                            UploadFailureKind.TooLarge -> R.string.feature_onboarding_impl_unreadable_explain_too_large
                            UploadFailureKind.Neutral -> R.string.feature_onboarding_impl_unreadable_explain_neutral
                        },
                    ),
                    style = TmrTheme.typography.body,
                    color = TmrTheme.colors.ink,
                )
            }
            TipsCard()
        }
        TmrBottomActionBar(stacked = true) {
            TmrSecondaryButton(
                label = stringResource(R.string.feature_onboarding_impl_unreadable_paste),
                onClick = onPasteClick,
                modifier = Modifier.fillMaxWidth(),
            )
            TmrPrimaryButton(
                label = stringResource(R.string.feature_onboarding_impl_unreadable_choose_another),
                onClick = onChooseAnotherClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TmrTheme.colors.surfaceRaised, TmrTheme.shapes.card)
            .padding(TmrTheme.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_onboarding_impl_unreadable_tips_title),
            style = TmrTheme.typography.label,
            color = TmrTheme.colors.textSecondary,
        )
        TipRow(R.string.feature_onboarding_impl_unreadable_tip_export)
        TipRow(R.string.feature_onboarding_impl_unreadable_tip_docx)
        TipRow(R.string.feature_onboarding_impl_unreadable_tip_paste)
    }
}

@Composable
private fun TipRow(textRes: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(TmrTheme.colors.lime, CircleShape)
                .padding(top = 6.dp),
        )
        Text(
            text = stringResource(textRes),
            modifier = Modifier.weight(1f),
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.text,
        )
    }
}

@Composable
internal fun UnreadableRoute(
    viewModel: UnreadableViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val failure by viewModel.uiState.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::onUriPicked)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UnreadableEvent.OpenReading -> navigator.replace(ReadingNavKey())
                UnreadableEvent.OpenPaste -> navigator.replace(PasteResumeNavKey())
            }
        }
    }
    UnreadableScreen(
        failure = failure,
        onChooseAnotherClick = { launcher.launch(resumeMimeTypes()) },
        onPasteClick = viewModel::onPasteAsText,
        modifier = modifier,
    )
}
