package com.tailormyresume.core.designsystem.component.content

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tailormyresume.core.designsystem.component.readAs
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = TmrTheme.typography.label,
        color = TmrTheme.colors.textMuted,
        modifier = modifier.readAs(text).semantics { heading() },
    )
}
