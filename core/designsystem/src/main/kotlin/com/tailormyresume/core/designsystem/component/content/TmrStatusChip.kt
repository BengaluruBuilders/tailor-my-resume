package com.tailormyresume.core.designsystem.component.content

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrApplicationStatus(@StringRes val labelRes: Int) {
    Saved(R.string.core_designsystem_content_status_saved),
    Applied(R.string.core_designsystem_content_status_applied),
    Interview(R.string.core_designsystem_content_status_interview),
    Offer(R.string.core_designsystem_content_status_offer),
    Rejected(R.string.core_designsystem_content_status_rejected),
}

@Immutable
data class TmrStatusChipColors(val text: Color, val fill: Color, val border: Color)

internal fun tmrStatusChipColors(status: TmrApplicationStatus, colors: TmrColors): TmrStatusChipColors =
    when (status) {
        TmrApplicationStatus.Saved -> TmrStatusChipColors(colors.blue, Color.Transparent, colors.blue)
        TmrApplicationStatus.Applied -> TmrStatusChipColors(colors.amber, Color.Transparent, colors.amber)
        TmrApplicationStatus.Interview -> TmrStatusChipColors(colors.ink, colors.lime, colors.lime)
        TmrApplicationStatus.Offer -> TmrStatusChipColors(colors.ink, colors.amber, colors.amber)
        TmrApplicationStatus.Rejected ->
            TmrStatusChipColors(colors.textMuted, Color.Transparent, colors.rejectedBorder)
    }

@Composable
fun TmrStatusChip(status: TmrApplicationStatus, modifier: Modifier = Modifier) {
    val chipColors = tmrStatusChipColors(status, TmrTheme.colors)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = chipColors.fill,
        border = BorderStroke(1.dp, chipColors.border),
    ) {
        Text(
            text = stringResource(status.labelRes),
            style = TmrTheme.typography.caption,
            color = chipColors.text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}
