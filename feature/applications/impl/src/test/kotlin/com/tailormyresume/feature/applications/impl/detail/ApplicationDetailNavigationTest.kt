package com.tailormyresume.feature.applications.impl.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.gateway.TestAccountWallet
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.impl.grant
import com.tailormyresume.feature.applications.impl.prototypeApplication
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ApplicationDetailNavigationTest {

    private val dispatcherRule = MainDispatcherRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(dispatcherRule).around(composeRule)

    private val applicationRepository = TestApplicationRepository()
    private val creditsRepository = TestCreditsRepository()
    private val detailKey = ApplicationDetailNavKey("kb")
    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(detailKey)))
    private val stack: List<NavKey> get() = navigator.state.stack.toList()

    private fun show(
        credits: Int,
        hasTailoredResume: Boolean = false,
        hasExportFile: Boolean = true,
    ) {
        val application = prototypeApplication("kb")
        applicationRepository.sendApplications(
            listOf(
                if (hasTailoredResume) {
                    application.copy(
                        tailoredResume = TailoredResume(emptyList()),
                        exportFileName = if (hasExportFile) application.exportFileName else null,
                    )
                } else {
                    application
                },
            ),
        )
        if (credits > 0) creditsRepository.sendLedger(listOf(grant(credits)))
        val viewModel = ApplicationDetailViewModel(
            applicationRepository = applicationRepository,
            creditBalance = AccountCreditBalance(TestAccountWallet().withFreeCredits(0), creditsRepository),
            setApplicationStatus = SetApplicationStatusUseCase(applicationRepository, TestClock()),
            tailorResume = TailorResumeUseCase(OfflineResumeTailor(), AllowAllFabricationGuard),
            exportedPdfIntents = FakeExportedPdfIntents(),
            applicationId = "kb",
        )
        composeRule.setContent { TmrTheme { ApplicationDetailRoute(viewModel, navigator) } }
        composeRule.waitForIdle()
    }

    @Test
    fun viewJobAnalysisPushesJobResult() {
        show(credits = 1)

        composeRule.onNodeWithText("View job analysis").performClick()

        assertThat(stack.last()).isEqualTo(JobResultNavKey("kb"))
    }

    @Test
    fun twoRetailorTapsPushTwoDifferentRunIds() {
        show(credits = 2)

        composeRule.onNodeWithText("Re-tailor with profile changes").performClick()
        composeRule.waitForIdle()
        val first = stack.last() as TailoringNavKey
        navigator.goBack()
        composeRule.onNodeWithText("Re-tailor with profile changes").performClick()
        composeRule.waitForIdle()
        val second = stack.last() as TailoringNavKey

        assertThat(first.applicationId).isEqualTo("kb")
        assertThat(second.applicationId).isEqualTo("kb")
        assertThat(second.runId).isNotEqualTo(first.runId)
    }

    @Test
    fun retailorWithoutCreditsPushesPaywallForThisApplication() {
        show(credits = 0)

        composeRule.onNodeWithText("Re-tailor with profile changes").performClick()
        composeRule.waitForIdle()

        assertThat(stack.last()).isEqualTo(CreditsNavKey(returnToApplicationId = "kb"))
    }

    @Test
    fun reviewUnexportedTailoredResumePushesTheTailoredScreen() {
        show(credits = 0, hasTailoredResume = true, hasExportFile = false)

        composeRule.onNodeWithText("Review tailored resume").performClick()

        assertThat(stack.last()).isEqualTo(TailoredNavKey("kb"))
    }

    @Test
    fun reviewExportedTailoredResumeKeepsTheFileCardAndPushesTheTailoredScreen() {
        show(credits = 0, hasTailoredResume = true)

        composeRule.onNodeWithText("Open").assertExists()
        composeRule.onNodeWithText("Review tailored resume").performClick()

        assertThat(stack.last()).isEqualTo(TailoredNavKey("kb"))
    }
}
