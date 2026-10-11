package com.tailormyresume.core.designsystem.component.chrome

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme
import kotlinx.coroutines.delay

private const val TOAST_DISPLAY_MS = 3200

@Composable
private fun defaultRecommendedTimeoutMillis(): (Int, Int) -> Int {
    val context = LocalContext.current
    return { original, flags ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(AccessibilityManager::class.java)
                ?.getRecommendedTimeoutMillis(original, flags) ?: original
        } else {
            original
        }
    }
}

@Composable
private fun defaultIsScreenReaderEnabled(): () -> Boolean {
    val context = LocalContext.current
    return {
        context.getSystemService(AccessibilityManager::class.java)
            ?.isTouchExplorationEnabled ?: false
    }
}

class TmrToastAction(val label: String, val onClick: () -> Unit)

class TmrToast(
    val id: Long,
    val message: String,
    val action: TmrToastAction?,
    val durationMillis: Int,
)

@Stable
class TmrToastState {
    var current: TmrToast? by mutableStateOf<TmrToast?>(null)
        private set

    private var lastId by mutableLongStateOf(0L)

    fun show(
        message: String,
        action: TmrToastAction? = null,
        durationMillis: Int = TOAST_DISPLAY_MS,
    ) {
        lastId += 1L
        current = TmrToast(
            id = lastId,
            message = message,
            action = action,
            durationMillis = durationMillis,
        )
    }

    fun dismiss() {
        current = null
    }
}

val LocalTmrToast = staticCompositionLocalOf { TmrToastState() }

@Composable
fun TmrToastHost(
    state: TmrToastState,
    modifier: Modifier = Modifier,
    recommendedTimeoutMillis: (Int, Int) -> Int = defaultRecommendedTimeoutMillis(),
    isScreenReaderEnabled: () -> Boolean = defaultIsScreenReaderEnabled(),
) {
    val toast = state.current ?: return
    val motion = TmrTheme.motion
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val shapes = TmrTheme.shapes
    val spacing = TmrTheme.spacing
    val displayMs: Int? =
        remember(toast.id) {
            if (toast.action != null && isScreenReaderEnabled()) {
                null
            } else if (toast.action != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                recommendedTimeoutMillis(
                    toast.durationMillis,
                    AccessibilityManager.FLAG_CONTENT_TEXT or AccessibilityManager.FLAG_CONTENT_CONTROLS,
                )
            } else {
                toast.durationMillis
            }
        }
    val progress = remember(toast.id) { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(toast.id) {
        val timeout = displayMs
        if (timeout != null) {
            delay(timeout.toLong())
            state.dismiss()
        }
    }
    if (!motion.reduced) {
        LaunchedEffect(toast.id) { progress.animateTo(1f, motion.proofSpecs.spatial) }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier =
            Modifier
                .padding(top = spacing.toastTop, start = spacing.gutter, end = spacing.gutter)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = progress.value.coerceIn(0f, 1f)
                    translationY = (1f - progress.value) * 12.dp.toPx()
                }.clip(shapes.toast)
                .background(colors.paper)
                .heightIn(min = 44.dp)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                .testTag(TmrChromeTags.TOAST)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    dismiss {
                        state.dismiss()
                        true
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = toast.message,
                style = typography.bodySmall,
                color = colors.ink,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
            val action = toast.action
            if (action != null) {
                Box(
                    modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) {
                            state.dismiss()
                            action.onClick()
                        }.testTag(TmrChromeTags.TOAST_ACTION),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .heightIn(min = 40.dp)
                            .clip(CircleShape)
                            .background(colors.ink)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = action.label, style = typography.button, color = colors.lime)
                    }
                }
            }
        }
    }
}
