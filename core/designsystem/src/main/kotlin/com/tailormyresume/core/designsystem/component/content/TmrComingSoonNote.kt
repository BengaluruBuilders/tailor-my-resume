package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrComingSoonNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.core_designsystem_content_coming_soon_note),
        style = TmrTheme.typography.bodySmall,
        color = TmrTheme.colors.textMuted,
        modifier = modifier.fillMaxWidth(),
    )
}
