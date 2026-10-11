package com.tailormyresume.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrSheet(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = TmrTheme.spacing.gutter,
        top = TmrTheme.spacing.d24,
        end = TmrTheme.spacing.gutter,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = TmrTheme.shapes.sheet,
        color = TmrTheme.colors.background,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Preview(showBackground = true)
@Composable
private fun TmrSheetDarkPreview() {
    TmrPreviewTheme { TmrSheetSample() }
}

@Composable
private fun TmrSheetSample() {
    TmrSheet {
        Text(text = "Your applications", style = TmrTheme.typography.titleL, color = TmrTheme.colors.onSurface)
    }
}
