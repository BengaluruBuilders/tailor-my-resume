package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme

@Composable
fun TmrLabeledField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = TmrTheme.typography.caption, color = TmrTheme.colors.textSecondary)
        content()
    }
}

@Composable
fun TmrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    amber: Boolean = false,
) {
    TmrLabeledField(label = label, modifier = modifier) {
        TmrFieldBox(value, onValueChange, label, placeholder, readOnly, amber, singleLine = true, minHeight = 56.dp)
    }
}

@Composable
fun TmrTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    amber: Boolean = false,
) {
    TmrLabeledField(label = label, modifier = modifier) {
        TmrFieldBox(value, onValueChange, label, placeholder, readOnly, amber, singleLine = false, minHeight = 120.dp)
    }
}

@Composable
private fun TmrFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    readOnly: Boolean,
    amber: Boolean,
    singleLine: Boolean,
    minHeight: Dp,
) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.field
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        textStyle = TmrTheme.typography.bodyLarge.copy(color = colors.text),
        cursorBrush = SolidColor(colors.lime),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(colors.fill, shape)
            .border(BorderStroke(1.5.dp, if (amber) colors.amber else colors.boundary), shape)
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = TmrTheme.typography.bodyLarge, color = colors.textDisabled)
                }
                inner()
            }
        },
    )
}
