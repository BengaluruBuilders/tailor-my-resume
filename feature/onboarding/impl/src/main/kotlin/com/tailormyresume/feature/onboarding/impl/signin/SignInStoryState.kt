package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tailormyresume.core.designsystem.theme.TmrTheme

internal const val SIGN_IN_STORY_COUNT = 3

internal const val STORY_HOLD_THRESHOLD_MS = 250L

private const val NANOS_PER_MILLI = 1_000_000L

@Stable
internal class SignInStoryState(private val durationMs: Int = 5000) {
    var index by mutableIntStateOf(0)
        private set

    var fraction by mutableFloatStateOf(0f)
        private set

    var paused by mutableStateOf(false)
        private set

    fun tick(dtMs: Long) {
        fraction += dtMs.toFloat() / durationMs
        while (fraction >= 1f) {
            fraction -= 1f
            index = (index + 1) % SIGN_IN_STORY_COUNT
        }
    }

    fun step(delta: Int) {
        index = Math.floorMod(index + delta, SIGN_IN_STORY_COUNT)
        fraction = 0f
    }

    fun onDown() {
        paused = true
    }

    fun onUp(heldMs: Long, x: Float, width: Float) {
        paused = false
        if (heldMs < STORY_HOLD_THRESHOLD_MS) step(if (x < width / 3f) -1 else 1)
    }

    fun onCancel() {
        paused = false
    }
}

@Composable
internal fun rememberSignInStoryState(signingIn: Boolean): SignInStoryState {
    val idle = TmrTheme.motion.idle
    val state = remember(idle.storyDurationMs) { SignInStoryState(idle.storyDurationMs) }
    val running = idle.storyAutoAdvance && !state.paused && !signingIn
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withInfiniteAnimationFrameNanos { it }
        while (true) {
            val now = withInfiniteAnimationFrameNanos { it }
            val elapsedMs = (now - last) / NANOS_PER_MILLI
            state.tick(elapsedMs)
            last += elapsedMs * NANOS_PER_MILLI
        }
    }
    return state
}
