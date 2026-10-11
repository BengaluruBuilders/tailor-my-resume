package com.tailormyresume.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class ChromeActions {
    var handler: (() -> Unit)? by mutableStateOf(null)
    var backHandler: (() -> Unit)? by mutableStateOf(null)
    var enabled: Boolean by mutableStateOf(true)
}

@Composable
fun RegisterChromeBack(onBack: () -> Unit) {
    val actions = LocalChromeActions.current
    val latest by rememberUpdatedState(onBack)
    DisposableEffect(actions) {
        val registered: () -> Unit = { latest() }
        actions.backHandler = registered
        onDispose {
            if (actions.backHandler === registered) actions.backHandler = null
        }
    }
}

val LocalChromeActions = staticCompositionLocalOf { ChromeActions() }

@Composable
fun RegisterChromeAction(
    onAction: () -> Unit,
    enabled: Boolean = true,
) {
    val actions = LocalChromeActions.current
    val latest by rememberUpdatedState(onAction)
    SideEffect { actions.enabled = enabled }
    DisposableEffect(actions) {
        val registered: () -> Unit = { latest() }
        actions.handler = registered
        onDispose {
            if (actions.handler === registered) {
                actions.handler = null
                actions.enabled = true
            }
        }
    }
}
