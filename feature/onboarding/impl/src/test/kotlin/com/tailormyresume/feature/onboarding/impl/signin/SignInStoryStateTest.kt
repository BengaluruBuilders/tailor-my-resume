package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignInStoryStateTest {

    private val state = SignInStoryState(durationMs = 5000)

    @Test
    fun autoAdvancesAt5000AndWraps() {
        state.tick(2500)
        assertThat(state.index).isEqualTo(0)
        assertThat(state.fraction).isWithin(0.001f).of(0.5f)

        state.tick(2500)
        assertThat(state.index).isEqualTo(1)
        assertThat(state.fraction).isWithin(0.001f).of(0f)

        state.tick(5000)
        state.tick(5000)
        assertThat(state.index).isEqualTo(0)
    }

    @Test
    fun tapLeftThirdGoesPreviousWrapping() {
        state.onDown()
        state.onUp(heldMs = 100, x = 50f, width = 300f)

        assertThat(state.index).isEqualTo(2)
        assertThat(state.paused).isFalse()
        assertThat(state.fraction).isEqualTo(0f)
    }

    @Test
    fun tapElsewhereNext() {
        state.onDown()
        state.onUp(heldMs = 100, x = 100f, width = 300f)
        assertThat(state.index).isEqualTo(1)

        state.onDown()
        state.onUp(heldMs = 100, x = 290f, width = 300f)
        assertThat(state.index).isEqualTo(2)
    }

    @Test
    fun holdPausesWithoutStep() {
        state.tick(1000)
        state.onDown()
        assertThat(state.paused).isTrue()

        state.onUp(heldMs = 400, x = 290f, width = 300f)

        assertThat(state.paused).isFalse()
        assertThat(state.index).isEqualTo(0)
        assertThat(state.fraction).isWithin(0.001f).of(0.2f)
    }

    @Test
    fun cancelResumes() {
        state.onDown()
        state.onCancel()

        assertThat(state.paused).isFalse()
        assertThat(state.index).isEqualTo(0)
    }
}
