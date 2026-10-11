package com.tailormyresume.core.designsystem.component.content

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.component.readAs
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrKeywordState(@StringRes val stateRes: Int) {
    Have(R.string.core_designsystem_content_keyword_have_state),
    Missing(R.string.core_designsystem_content_keyword_missing_state),
}

enum class TmrSource(@StringRes val labelRes: Int) {
    YourResume(R.string.core_designsystem_content_source_your_resume),
    YourAnswer(R.string.core_designsystem_content_source_your_answer),
}

enum class TmrTag(@StringRes val labelRes: Int) {
    Soon(R.string.core_designsystem_content_tag_soon),
    Add(R.string.core_designsystem_content_tag_add),
    BestValue(R.string.core_designsystem_content_tag_best_value),
    Required(R.string.core_designsystem_content_tag_required),
    New(R.string.core_designsystem_content_tag_new),
    Rewritten(R.string.core_designsystem_content_tag_rewritten),
    Added(R.string.core_designsystem_content_tag_added),
    Reordered(R.string.core_designsystem_content_tag_reordered),
}

@Immutable
data class TmrChipStyle(val text: Color, val fill: Color, val border: Color)

internal fun tmrKeywordChipStyle(state: TmrKeywordState, colors: TmrColors): TmrChipStyle =
    when (state) {
        TmrKeywordState.Have -> TmrChipStyle(colors.text, colors.fill, Color.Transparent)
        TmrKeywordState.Missing -> TmrChipStyle(colors.amber, Color.Transparent, colors.amber)
    }

internal fun tmrSourceColor(source: TmrSource, colors: TmrColors): Color =
    when (source) {
        TmrSource.YourResume -> colors.lime
        TmrSource.YourAnswer -> colors.amber
    }

internal fun tmrTagStyle(tag: TmrTag, colors: TmrColors): TmrChipStyle =
    when (tag) {
        TmrTag.Soon -> TmrChipStyle(colors.textMuted, Color.Transparent, colors.boundary)
        TmrTag.Add -> TmrChipStyle(colors.ink, colors.amber, colors.amber)
        TmrTag.BestValue -> TmrChipStyle(colors.ink, colors.lime, colors.lime)
        TmrTag.Required -> TmrChipStyle(colors.amber, Color.Transparent, colors.amber)
        TmrTag.New, TmrTag.Added, TmrTag.Rewritten, TmrTag.Reordered ->
            TmrChipStyle(colors.lime, colors.limeSelected, colors.lime)
    }

@Composable
fun TmrKeywordChip(label: String, state: TmrKeywordState, modifier: Modifier = Modifier) {
    val style = tmrKeywordChipStyle(state, TmrTheme.colors)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = style.fill,
        border = BorderStroke(1.dp, style.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (state == TmrKeywordState.Have) {
                Box(Modifier.size(6.dp).background(TmrTheme.colors.lime, CircleShape))
            }
            val stateText = stringResource(state.stateRes)
            Text(
                text = label,
                style = TmrTheme.typography.caption,
                color = style.text,
                modifier = Modifier.semantics { stateDescription = stateText },
            )
        }
    }
}

@Composable
fun TmrSourceChip(source: TmrSource, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(source.labelRes),
        style = TmrTheme.typography.caption,
        color = tmrSourceColor(source, TmrTheme.colors),
        modifier = modifier,
    )
}

@Composable
fun TmrTagChip(tag: TmrTag, modifier: Modifier = Modifier) {
    val style = tmrTagStyle(tag, TmrTheme.colors)
    val label = stringResource(tag.labelRes)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = style.fill,
        border = BorderStroke(1.dp, style.border),
    ) {
        Text(
            text = label.uppercase(),
            style = TmrTheme.typography.label,
            color = style.text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp).readAs(label),
        )
    }
}
