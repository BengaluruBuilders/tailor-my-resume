package com.tailormyresume.feature.analysis.impl.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrBottomActionBar
import com.tailormyresume.core.designsystem.component.TmrCard
import com.tailormyresume.core.designsystem.component.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.TmrWholeWordText
import com.tailormyresume.core.designsystem.component.content.TmrCoverageDelta
import com.tailormyresume.core.designsystem.component.content.TmrKeywordChip
import com.tailormyresume.core.designsystem.component.content.TmrKeywordState
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.analysis.impl.R

private val MARKER_SIZE = 20.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JobResultScreen(
    state: JobResultUiState,
    onTailor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background),
    ) {
        when (state) {
            JobResultUiState.Loading -> Box(Modifier.fillMaxSize())

            JobResultUiState.Unavailable -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.feature_analysis_impl_result_unavailable),
                    style = TmrTheme.typography.body,
                    color = TmrTheme.colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }

            is JobResultUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ResultHeader(state)
                    CoverageCard(state)
                    KeywordsCard(state)
                    MustHavesCard(state)
                }
                JobResultBottom(state, onTailor)
            }
        }
    }
}

@Composable
private fun ResultHeader(state: JobResultUiState.Ready) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TmrWholeWordText(
            text = state.title,
            style = TmrTheme.typography.headline,
            color = TmrTheme.colors.text,
        )
        TmrWholeWordText(
            text = state.subtitle,
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.textMuted,
        )
    }
}

@Composable
private fun CoverageCard(state: JobResultUiState.Ready) {
    TmrCard {
        TmrCoverageDelta(state.now, state.upTo)
        Text(
            text = stringResource(R.string.feature_analysis_impl_result_backed),
            style = TmrTheme.typography.caption,
            color = TmrTheme.colors.textMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordsCard(state: JobResultUiState.Ready) {
    TmrCard {
        TmrSectionLabel(stringResource(R.string.feature_analysis_impl_result_screen_for, state.keywordCount))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.have.forEach { keyword ->
                TmrKeywordChip(label = keyword, state = TmrKeywordState.Have)
            }
            state.missing.forEach { keyword ->
                TmrKeywordChip(label = keyword, state = TmrKeywordState.Missing)
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(
                label = stringResource(R.string.feature_analysis_impl_result_legend_have),
                outlined = false,
            )
            LegendItem(
                label = stringResource(R.string.feature_analysis_impl_result_legend_missing),
                outlined = true,
            )
        }
        Text(
            text = stringResource(R.string.feature_analysis_impl_result_confirm_first),
            style = TmrTheme.typography.caption,
            color = TmrTheme.colors.textMuted,
        )
    }
}

@Composable
private fun LegendItem(label: String, outlined: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val dot = Modifier.size(6.dp)
        if (outlined) {
            Box(dot.border(1.dp, TmrTheme.colors.amber, CircleShape))
        } else {
            Box(dot.background(TmrTheme.colors.lime, CircleShape))
        }
        Text(
            text = label,
            style = TmrTheme.typography.caption,
            color = TmrTheme.colors.textMuted,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun MustHavesCard(state: JobResultUiState.Ready) {
    TmrCard {
        TmrSectionLabel(stringResource(R.string.feature_analysis_impl_result_must_haves))
        state.mustHaves.forEach { row ->
            MustHaveItem(row)
        }
    }
}

@Composable
private fun MustHaveItem(row: MustHaveRow) {
    Row(modifier = Modifier.fillMaxWidth()) {
        MustHaveMarker(row)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TmrWholeWordText(
                text = row.text,
                style = TmrTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TmrTheme.colors.text,
            )
            val detail = when {
                row.unclear -> stringResource(R.string.feature_analysis_impl_result_unclear)
                else -> row.reason
            }
            if (detail != null) {
                TmrWholeWordText(
                    text = detail,
                    style = TmrTheme.typography.caption,
                    color = TmrTheme.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun MustHaveMarker(row: MustHaveRow) {
    val colors = TmrTheme.colors
    val size = Modifier.size(MARKER_SIZE)
    val (fill, glyph) = when (row.marker) {
        MustHaveMarkerKind.UNCLEAR -> colors.amber to stringResource(R.string.feature_analysis_impl_result_unclear_mark)
        MustHaveMarkerKind.MET -> colors.lime to stringResource(R.string.feature_analysis_impl_result_met_mark)
        MustHaveMarkerKind.PARTIAL -> colors.limeSoft to stringResource(R.string.feature_analysis_impl_result_partial_mark)
        MustHaveMarkerKind.GAP -> colors.coral to stringResource(R.string.feature_analysis_impl_result_unclear_mark)
    }
    val status = stringResource(
        when (row.marker) {
            MustHaveMarkerKind.UNCLEAR -> R.string.feature_analysis_impl_result_status_unclear
            MustHaveMarkerKind.MET -> R.string.feature_analysis_impl_result_legend_have
            MustHaveMarkerKind.PARTIAL -> R.string.feature_analysis_impl_result_status_partial
            MustHaveMarkerKind.GAP -> R.string.feature_analysis_impl_result_legend_missing
        },
    )
    Box(
        modifier = size
            .background(fill, CircleShape)
            .semantics { contentDescription = status },
        contentAlignment = Alignment.Center,
    ) {
        MarkerGlyph(glyph)
    }
}

@Composable
private fun MarkerGlyph(glyph: String) {
    val glyphSize = with(LocalDensity.current) { MARKER_SIZE.value.times(0.5f).dp.toSp() }
    Text(
        text = glyph,
        style = TmrTheme.typography.caption.copy(fontSize = glyphSize, lineHeight = glyphSize),
        color = TmrTheme.colors.ink,
    )
}

@Composable
private fun JobResultBottom(state: JobResultUiState.Ready, onTailor: () -> Unit) {
    val note = if (state.credits > 0) {
        stringResource(R.string.feature_analysis_impl_result_note_credits, state.credits)
    } else {
        stringResource(R.string.feature_analysis_impl_result_note_no_credits)
    }
    TmrBottomActionBar(
        creditDisclosure = {
            Text(
                text = note,
                style = TmrTheme.typography.caption,
                color = TmrTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        TmrPrimaryButton(
            label = stringResource(R.string.feature_analysis_impl_result_tailor),
            onClick = onTailor,
            modifier = Modifier.weight(1f),
        )
    }
}
