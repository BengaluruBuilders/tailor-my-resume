package com.tailormyresume.feature.analysis.impl.job

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.component.hero.TmrHeroCard
import com.tailormyresume.core.designsystem.component.hero.TmrHeroColor
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.input.TmrInputModeOption
import com.tailormyresume.core.designsystem.component.input.TmrInputModeSelector
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.component.input.TmrTextArea
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.analysis.impl.R
import java.text.NumberFormat

private const val STACKED_BUTTON_FONT_SCALE = 1.3f

@Composable
internal fun JobScreen(
    state: JobUiState,
    onTextChange: (String) -> Unit,
    onUseLink: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val fontScale = LocalDensity.current.fontScale
    val pinBottom = fontScale <= STACKED_BUTTON_FONT_SCALE
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        BoxWithConstraints(Modifier.weight(1f)) {
            val fieldHeight = if (pinBottom) {
                (maxHeight - if (state is JobUiState.Empty) 280.dp else 140.dp).coerceAtLeast(240.dp)
            } else {
                240.dp
            }
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp)
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state is JobUiState.Empty) {
                    JobInputHero(showArt = pinBottom)
                } else {
                    JobHeader(showClear = state is JobUiState.HasText, onClear = onClear, fontScale = fontScale)
                }
                TmrInputModeSelector(
                    options = jobInputModes(),
                    selectedIndex = 0,
                    onSelect = { if (it == 1) onUseLink() },
                    enabled = state !is JobUiState.Analyzing,
                )
                TmrTextArea(
                    value = state.text,
                    onValueChange = onTextChange,
                    label = stringResource(R.string.feature_analysis_impl_job_field_label),
                    placeholder = stringResource(R.string.feature_analysis_impl_job_placeholder),
                    readOnly = state is JobUiState.Analyzing,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = fieldHeight,
                    footer = {
                        if (state !is JobUiState.Analyzing) {
                            JobCharCount(state.text)
                        }
                    },
                )
                if (!pinBottom) JobBottom(state, onAnalyze)
            }
        }
        if (pinBottom) JobBottom(state, onAnalyze)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobBottom(state: JobUiState, onAnalyze: () -> Unit) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
    ) {
        when (state) {
            JobUiState.Empty -> {
                TmrPrimaryButton(
                    label = stringResource(R.string.feature_analysis_impl_job_analyze),
                    onClick = onAnalyze,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is JobUiState.HasText -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JobStatusRow(state)
                if (state.notAJobPost) JobNotAPostCard()
                TmrPrimaryButton(
                    label = stringResource(R.string.feature_analysis_impl_job_analyze),
                    onClick = onAnalyze,
                    enabled = state.text.trim().length >= MIN_JOB_CHARS && !state.notAJobPost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is JobUiState.Analyzing -> JobAnalyzingPill(percent = state.percent)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JobInputHero(showArt: Boolean) {
    TmrHeroCard(
        color = TmrHeroColor.Amber,
        label = stringResource(R.string.feature_analysis_impl_job_hero_label),
        headline = stringResource(R.string.feature_analysis_impl_job_hero_headline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 136.dp),
    ) {
        if (!showArt) return@TmrHeroCard
        TmrPaige(
            TmrPaigePose.Job,
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp)
                .offset(y = 54.dp)
                .scale(0.7f),
        )
        JobMagnifier(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 166.dp)
                .offset(y = 58.dp),
        )
    }
}

@Composable
private fun JobMagnifier(modifier: Modifier = Modifier) {
    val ink = TmrTheme.colors.ink
    Canvas(modifier = modifier.size(width = 92.dp, height = 84.dp)) {
        val radius = 26.dp.toPx()
        val center = Offset(34.dp.toPx(), 34.dp.toPx())
        drawCircle(color = Color.White.copy(alpha = 0.45f), radius = radius, center = center)
        drawCircle(color = ink, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
        val handleStart = Offset(center.x + radius * 0.72f, center.y + radius * 0.72f)
        rotate(degrees = -45f, pivot = handleStart) {
            drawRoundRect(
                color = ink,
                topLeft = handleStart,
                size = Size(width = 28.dp.toPx(), height = 9.dp.toPx()),
                cornerRadius = CornerRadius(4.5.dp.toPx()),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobHeader(showClear: Boolean, onClear: () -> Unit, fontScale: Float) {
    val style = TmrTheme.typography.headline
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_analysis_impl_job_title),
            style = style.copy(
                fontSize = (style.fontSize.value * minOf(fontScale, 1.4f) / fontScale).sp,
            ),
            color = TmrTheme.colors.text,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (showClear) {
            TmrSecondaryButton(
                label = stringResource(R.string.feature_analysis_impl_job_clear),
                onClick = onClear,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun JobStatusRow(state: JobUiState.HasText) {
    val detected = state.detected ?: return
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        JobDetectedMark()
        Text(
            text = detected,
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.text,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun JobCharCount(text: String) {
    val count = text.trim().length
    val format = remember { NumberFormat.getIntegerInstance() }
    val shown = format.format(count)
    val minimum = format.format(MIN_JOB_CHARS)
    val spoken = if (count == 0) {
        stringResource(R.string.feature_analysis_impl_job_char_count_empty, minimum)
    } else {
        stringResource(R.string.feature_analysis_impl_job_char_count_description, shown, minimum)
    }
    Text(
        text = if (count == 0) {
            stringResource(R.string.feature_analysis_impl_job_char_count_empty, minimum)
        } else {
            stringResource(R.string.feature_analysis_impl_job_char_count, shown, minimum)
        },
        style = TmrTheme.typography.strongSmall,
        color = if (count >= MIN_JOB_CHARS) TmrTheme.colors.lime else TmrTheme.colors.textMuted,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = spoken },
    )
}

@Composable
internal fun jobInputModes(): List<TmrInputModeOption> = listOf(
    TmrInputModeOption(stringResource(R.string.feature_analysis_impl_job_paste), TmrIcons.Description),
    TmrInputModeOption(stringResource(R.string.feature_analysis_impl_job_use_link), TmrIcons.Link),
)

@Composable
private fun JobDetectedMark() {
    val lime = TmrTheme.colors.lime
    val ink = TmrTheme.colors.ink
    Canvas(modifier = Modifier.size(20.dp)) {
        drawCircle(color = lime, radius = size.minDimension / 2f)
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(
            color = ink,
            start = Offset(size.width * 0.26f, size.height * 0.52f),
            end = Offset(size.width * 0.44f, size.height * 0.70f),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
        drawLine(
            color = ink,
            start = Offset(size.width * 0.44f, size.height * 0.70f),
            end = Offset(size.width * 0.76f, size.height * 0.32f),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
    }
}

@Composable
private fun JobNotAPostCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TmrTheme.colors.amber)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_analysis_impl_job_not_a_post_title),
            style = TmrTheme.typography.body.copy(fontWeight = FontWeight.Bold),
            color = TmrTheme.colors.ink,
        )
        Text(
            text = stringResource(R.string.feature_analysis_impl_job_not_a_post_body),
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.ink,
        )
    }
}

@Composable
private fun JobAnalyzingPill(percent: Int) {
    val fill = TmrTheme.colors.limeSelected
    val fraction = (percent / 100f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(TmrTheme.colors.surfaceHigh)
            .drawBehind { drawRect(color = fill, size = Size(size.width * fraction, size.height)) }
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.feature_analysis_impl_job_analyzing, percent),
            style = TmrTheme.typography.mono14,
            color = TmrTheme.colors.lime,
            textAlign = TextAlign.Center,
        )
    }
}
