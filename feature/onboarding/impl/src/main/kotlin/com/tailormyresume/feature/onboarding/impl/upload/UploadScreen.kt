package com.tailormyresume.feature.onboarding.impl.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.hero.TmrHeroCard
import com.tailormyresume.core.designsystem.component.hero.TmrHeroColor
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.hero.TmrSticker
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.component.input.TmrUploadCard
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ManualProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.impl.R
import com.tailormyresume.feature.onboarding.impl.importresume.resumeMimeTypes

private val HERO_MIN_HEIGHT = 290.dp
private val HERO_EXTRA_PER_FONT_STEP = 140.dp

@Composable
internal fun UploadScreen(
    onUploadClick: () -> Unit,
    onPasteClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(TmrTheme.colors.background)) {
        val screenHeight = maxHeight
        val heroMinHeight = HERO_MIN_HEIGHT + HERO_EXTRA_PER_FONT_STEP * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)
        Column(
            Modifier
                .heightIn(min = screenHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TmrHeroCard(
                color = TmrHeroColor.Blue,
                label = stringResource(R.string.feature_onboarding_impl_upload_hero_label),
                headline = stringResource(R.string.feature_onboarding_impl_upload_hero_headline),
                modifier = Modifier.heightIn(min = heroMinHeight),
            ) {
                TmrSticker(
                    text = stringResource(R.string.feature_onboarding_impl_upload_sticker_pdf),
                    rotationDegrees = -10f,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, bottom = 46.dp),
                )
                TmrSticker(
                    text = stringResource(R.string.feature_onboarding_impl_upload_sticker_docx),
                    rotationDegrees = 7f,
                    onCheek = true,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 58.dp, bottom = 100.dp),
                )
                TmrPaige(
                    pose = TmrPaigePose.Upload,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-34).dp, y = 26.dp),
                )
            }
            Text(
                text = stringResource(R.string.feature_onboarding_impl_upload_intro),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textSecondary,
            )
            TmrUploadCard(
                title = stringResource(R.string.feature_onboarding_impl_upload_card_title),
                hint = stringResource(R.string.feature_onboarding_impl_upload_card_hint),
                onClick = onUploadClick,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TmrSecondaryButton(
                    label = stringResource(R.string.feature_onboarding_impl_upload_paste),
                    onClick = onPasteClick,
                    modifier = Modifier.weight(1f),
                )
                TmrSecondaryButton(
                    label = stringResource(R.string.feature_onboarding_impl_upload_manual),
                    onClick = onManualClick,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.feature_onboarding_impl_upload_footer),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = TmrTheme.typography.caption,
                color = TmrTheme.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun UploadRoute(
    viewModel: UploadViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::onUriPicked)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { navigator.navigate(ReadingNavKey()) }
    }
    UploadScreen(
        onUploadClick = { launcher.launch(resumeMimeTypes()) },
        onPasteClick = { navigator.navigate(PasteResumeNavKey()) },
        onManualClick = { navigator.navigate(ManualProfileNavKey()) },
        modifier = modifier,
    )
}
