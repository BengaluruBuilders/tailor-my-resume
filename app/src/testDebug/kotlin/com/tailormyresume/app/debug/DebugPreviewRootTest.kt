package com.tailormyresume.app.debug

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ui.AppRootState
import com.tailormyresume.core.domain.onboarding.StartDestination
import org.junit.Test

class DebugPreviewRootTest {

    private val signedOut = AppRootState.Ready(StartDestination.SignIn, null)
    private val applications = AppRootState.Ready(StartDestination.Applications, "account-1")

    @Test
    fun previewShowsWelcome_onlyAfterMainWasSeenAndRootIsFirstRun() {
        assertThat(previewShowsWelcome(seenMain = true, rootState = signedOut)).isTrue()
        assertThat(previewShowsWelcome(seenMain = false, rootState = signedOut)).isFalse()
        assertThat(previewShowsWelcome(seenMain = true, rootState = applications)).isFalse()
        assertThat(previewShowsWelcome(seenMain = true, rootState = AppRootState.Loading)).isFalse()
        assertThat(previewShowsWelcome(seenMain = false, rootState = applications)).isFalse()
    }
}
