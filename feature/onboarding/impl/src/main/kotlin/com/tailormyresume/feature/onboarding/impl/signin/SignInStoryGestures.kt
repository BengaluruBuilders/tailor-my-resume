package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.signInStoryGestures(state: SignInStoryState): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            state.onDown()
            var movedPastTouchSlop = false
            var upTime = down.uptimeMillis
            var upX = down.position.x
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: continue
                movedPastTouchSlop = movedPastTouchSlop ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                if (change.changedToUpIgnoreConsumed()) {
                    upTime = change.uptimeMillis
                    upX = change.position.x
                    break
                }
            }
            if (movedPastTouchSlop) {
                state.onCancel()
            } else {
                state.onUp(upTime - down.uptimeMillis, upX, size.width.toFloat())
            }
        }
    }
