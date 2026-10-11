package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.signInStoryGestures(state: SignInStoryState): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            state.onDown()
            val up = waitForUpOrCancellation()
            if (up == null) {
                state.onCancel()
            } else {
                state.onUp(up.uptimeMillis - down.uptimeMillis, up.position.x, size.width.toFloat())
            }
        }
    }
