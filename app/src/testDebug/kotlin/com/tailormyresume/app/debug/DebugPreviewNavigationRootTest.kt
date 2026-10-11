package com.tailormyresume.app.debug

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ui.AppRootState
import com.tailormyresume.core.domain.onboarding.StartDestination
import org.junit.Test

class DebugPreviewNavigationRootTest {

    private val signedOut = AppRootState.Ready(StartDestination.SignIn, null)
    private val applications = AppRootState.Ready(StartDestination.Applications, "account-1")

    private fun root(
        opensFirstRunRoot: Boolean = false,
        seenMain: Boolean = false,
        rootState: AppRootState = applications,
        seenAccount: Boolean = false,
        hasAccount: Boolean = false,
    ) = previewNavigationRoot(opensFirstRunRoot, seenMain, rootState, seenAccount, hasAccount)

    @Test
    fun aFirstRunTargetShowsTheFirstRunRoot() {
        assertThat(root(opensFirstRunRoot = true)).isEqualTo(PreviewRoot.FirstRun)
    }

    @Test
    fun aMainTargetShowsTheMainRoot() {
        assertThat(root()).isEqualTo(PreviewRoot.Main)
    }

    @Test
    fun aSeenMainThatBecomesFirstRunShowsTheFirstRunRoot() {
        assertThat(root(seenMain = true, rootState = signedOut)).isEqualTo(PreviewRoot.FirstRun)
    }

    @Test
    fun anAccountThatGoesAwayShowsTheFirstRunRootEvenWhenOnboardingWasNeverComplete() {
        val shown = root(seenMain = false, rootState = signedOut, seenAccount = true, hasAccount = false)

        assertThat(shown).isEqualTo(PreviewRoot.FirstRun)
    }

    @Test
    fun anAccountThatIsStillThereKeepsTheMainRoot() {
        assertThat(root(seenAccount = true, hasAccount = true)).isEqualTo(PreviewRoot.Main)
    }

    @Test
    fun aPreviewOpenedSignedOutKeepsTheMainRoot() {
        assertThat(root(seenAccount = false, hasAccount = false)).isEqualTo(PreviewRoot.Main)
    }
}
