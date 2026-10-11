package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import com.tailormyresume.core.designsystem.component.input.TmrChoiceRow
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrStatusSheet(
    selected: TmrApplicationStatus,
    onSelect: (TmrApplicationStatus) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.core_designsystem_chrome_status_sheet_title)
    TmrBottomSheet(onDismiss = onDismiss, title = title, modifier = modifier) {
        Text(
            text = title,
            style = TmrTheme.typography.title,
            color = TmrTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TmrApplicationStatus.entries.forEach { status ->
                TmrChoiceRow(
                    label = stringResource(status.labelRes),
                    selected = status == selected,
                    onClick = { onSelect(status) },
                    modifier = Modifier.fillMaxWidth(),
                    onSheet = true,
                )
            }
        }
        TmrPrimaryButton(
            label = stringResource(R.string.core_designsystem_chrome_status_sheet_save),
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
