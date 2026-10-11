package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDisabledClick: () -> Unit = {},
) {
    val colors = TmrTheme.colors
    val unavailable = stringResource(R.string.core_designsystem_input_not_available)
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(TmrTheme.shapes.pill)
            .background(if (enabled) colors.lime else colors.disabledFill, TmrTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = if (enabled) onClick else onDisabledClick)
            .semantics { if (!enabled) stateDescription = unavailable }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TmrTheme.typography.mono15,
            color = if (enabled) colors.ink else colors.textDisabled,
            textAlign = TextAlign.Center,
        )
    }
}
