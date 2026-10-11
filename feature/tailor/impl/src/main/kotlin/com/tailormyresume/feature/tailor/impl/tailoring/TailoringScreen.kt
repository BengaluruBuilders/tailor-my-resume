package com.tailormyresume.feature.tailor.impl.tailoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.content.TmrProgressRow
import com.tailormyresume.core.designsystem.component.content.TmrProgressRows
import com.tailormyresume.core.designsystem.component.hero.TmrHeroCard
import com.tailormyresume.core.designsystem.component.hero.TmrHeroColor
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.hero.TmrSticker
import com.tailormyresume.feature.tailor.impl.R

private const val HERO_HEIGHT_DP = 286
private const val PAIGE_MAX_FONT_SCALE = 1.3f

private val StickerRotations = listOf(-8f, 7f, 5f)

private val StickerOffsets = listOf(
    PaddingValues(start = 12.dp, bottom = 12.dp),
    PaddingValues(start = 64.dp, bottom = 14.dp),
    PaddingValues(start = 12.dp, bottom = 54.dp),
)

@Composable
private fun tailoringRowLabel(kind: TailoringRowKind, keywordCount: Int): String = when (kind) {
    TailoringRowKind.Matching -> pluralStringResource(
        R.plurals.feature_tailor_impl_result_tailoring_row_matching,
        keywordCount,
        keywordCount,
    )

    TailoringRowKind.Rewriting -> stringResource(R.string.feature_tailor_impl_result_tailoring_row_rewriting)
    TailoringRowKind.AddingExample -> stringResource(R.string.feature_tailor_impl_result_tailoring_row_adding_example)
    TailoringRowKind.CheckingMustHaves -> stringResource(R.string.feature_tailor_impl_result_tailoring_row_checking)
    TailoringRowKind.Fitting -> stringResource(R.string.feature_tailor_impl_result_tailoring_row_fitting)
}

@Composable
internal fun TailoringScreen(state: TailoringUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TmrHeroCard(
            color = TmrHeroColor.Lime,
            label = stringResource(R.string.feature_tailor_impl_result_tailoring_label),
            headline = stringResource(R.string.feature_tailor_impl_result_tailoring_headline),
            modifier = Modifier
                .fillMaxWidth()
                .height((HERO_HEIGHT_DP * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp),
        ) {
            val enlarged = LocalDensity.current.fontScale > PAIGE_MAX_FONT_SCALE
            if (enlarged) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.stickers.take(3).forEachIndexed { index, sticker ->
                        TmrSticker(text = sticker, rotationDegrees = StickerRotations[index])
                    }
                }
            } else {
                state.stickers.take(3).forEachIndexed { index, sticker ->
                    TmrSticker(
                        text = sticker,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(StickerOffsets[index]),
                        rotationDegrees = StickerRotations[index],
                    )
                }
            }
            if (!enlarged) {
                TmrPaige(
                    pose = TmrPaigePose.Tailoring,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp),
                )
            }
        }
        TmrProgressRows(
            rows = state.rows.map { TmrProgressRow(label = tailoringRowLabel(it.kind, state.keywordCount), state = it.state) },
            percent = state.percent,
        )
    }
}
