package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class TmrHostedSheet(
    val title: String,
    val content: @Composable ColumnScope.() -> Unit,
)

@Stable
class TmrSheetHostState {
    var current: TmrHostedSheet? by mutableStateOf<TmrHostedSheet?>(null)
        private set

    var dismissible: Boolean by mutableStateOf(true)

    fun show(
        title: String,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        dismissible = true
        current = TmrHostedSheet(title, content)
    }

    fun dismiss() {
        if (dismissible) current = null
    }
}

val LocalTmrSheetHost = staticCompositionLocalOf { TmrSheetHostState() }

@Composable
fun TmrSheetHost(state: TmrSheetHostState) {
    val sheet = state.current ?: return
    TmrBottomSheet(onDismiss = state::dismiss, title = sheet.title, dismissible = state.dismissible, content = sheet.content)
}
