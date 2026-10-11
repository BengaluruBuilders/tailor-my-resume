package com.tailormyresume.feature.tailor.impl.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
internal fun NavKeyPlaceholder(
    key: NavKey,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = key::class.simpleName.orEmpty(),
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.text,
        )
    }
}
