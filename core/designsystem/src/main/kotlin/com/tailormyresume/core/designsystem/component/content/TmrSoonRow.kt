package com.tailormyresume.core.designsystem.component.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R

@Composable
fun TmrSoonRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    TmrListRow(
        label = label,
        modifier = modifier,
        tag = TmrTag.Soon,
        onClick = onClick,
        showChevron = false,
        showDivider = showDivider,
        stateDescription = stringResource(R.string.core_designsystem_content_soon_state),
        minHeight = 56.dp,
    )
}
