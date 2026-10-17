package com.tailormyresume.feature.applications.impl

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.gateway.TestAccountWallet
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ApplicationsNavigationTest {

    private val dispatcherRule = MainDispatcherRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(dispatcherRule).around(composeRule)

    private val applicationRepository = TestApplicationRepository()
    private val creditsRepository = TestCreditsRepository()
    private val profileRepository = TestProfileRepository()
    private val scenario = PrototypeFixtures.returning()
    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(DefaultApplicationsNavKey)))

    private fun show(withApplications: Boolean) {
        profileRepository.sendProfile(scenario.profile)
        creditsRepository.sendLedger(scenario.ledger)
        if (withApplications) applicationRepository.sendApplications(scenario.applications)
        val viewModel = ApplicationsViewModel(
            applicationRepository,
            AccountCreditBalance(TestAccountWallet().withFreeCredits(0), creditsRepository),
            profileRepository,
        )
        composeRule.setContent { TmrTheme { ApplicationsRoute(viewModel, navigator) } }
        composeRule.waitForIdle()
    }

    private val stack: List<NavKey> get() = navigator.state.stack.toList()

    @Test
    fun creditsAvatarAndAddJobNavigate() {
        show(withApplications = false)

        composeRule.onNodeWithText("3 credits").performClick()
        assertThat(stack.last()).isEqualTo(CreditsNavKey())

        navigator.root(DefaultApplicationsNavKey)
        composeRule.onNodeWithContentDescription("Profile").performClick()
        assertThat(stack).containsExactly(DefaultProfileNavKey)

        navigator.root(DefaultApplicationsNavKey)
        composeRule.onNodeWithText("Add a job").performClick()
        assertThat(stack.last()).isEqualTo(JobNavKey())
        assertThat(stack.first()).isInstanceOf(ApplicationsNavKey::class.java)
    }

    @Test
    fun rowTapPushesApplicationDetail() {
        show(withApplications = true)

        composeRule.onNodeWithText("Business Analyst").performClick()

        assertThat(stack.last()).isEqualTo(ApplicationDetailNavKey("kb"))
    }
}
