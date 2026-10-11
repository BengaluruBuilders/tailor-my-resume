package com.tailormyresume.feature.settings.impl.navigation

import android.app.Application
import android.content.ComponentName
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseOutcome
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

private const val BUY_FIFTEEN = "Buy 15 credits · ₹449"

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class PaywallRouteWiringTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<SettingsEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, SettingsEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var gateway: PaymentGateway

    @Inject
    lateinit var credits: CreditsRepository

    private lateinit var harness: EntryHarness

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun showWith(vararg stack: NavKey) {
        harness = EntryHarness(composeRule, stack.toList())
        harness.show()
        harness.waitFor { composeRule.onAllNodesWithText(BUY_FIFTEEN).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun buy() {
        composeRule.onNodeWithText(BUY_FIFTEEN).performClick()
    }

    @Test
    fun successReplacesPaywallWithQuickQuestionWhenReturnSet() {
        showWith(PaywallNavKey("app-1"))

        buy()

        harness.waitFor { harness.state.stack.last() == QuickQuestionNavKey("app-1") }
        assertThat(harness.toast.current?.message).isEqualTo("15 credits added")
        assertThat(runBlocking { credits.observeBalance().first() }).isEqualTo(15)
    }

    @Test
    fun successGoesBackWhenNoReturn() {
        showWith(CreditsNavKey(), PaywallNavKey(null))

        buy()

        harness.waitFor { harness.state.stack.last() == CreditsNavKey() }
        assertThat(harness.state.stack).containsExactly(CreditsNavKey())
    }

    @Test
    fun replaceKeepsOpenerBelowQuickQuestion() {
        showWith(JobNavKey(), JobResultNavKey("app-1"), PaywallNavKey("app-1"))

        buy()

        harness.waitFor { harness.state.stack.last() == QuickQuestionNavKey("app-1") }
        assertThat(harness.state.stack)
            .containsExactly(JobNavKey(), JobResultNavKey("app-1"), QuickQuestionNavKey("app-1"))
            .inOrder()
    }

    @Test
    fun creditsOpenedPaywallGoesBackToCredits() {
        showWith(SettingsNavKey(), CreditsNavKey(), PaywallNavKey(null))

        buy()

        harness.waitFor { harness.state.stack.last() == CreditsNavKey() }
        assertThat(harness.state.stack).containsExactly(SettingsNavKey(), CreditsNavKey()).inOrder()
    }

    @Test
    fun failureSheetTryAgainBuysAgain() {
        (gateway as TestPaymentGateway).withOutcome(ApplicationPack.APPLICATION_PACK_FIFTEEN, PurchaseOutcome.Failed)
        showWith(CreditsNavKey(), PaywallNavKey(null))

        buy()

        harness.waitFor { composeRule.onAllNodesWithText("Payment didn't go through").fetchSemanticsNodes().isNotEmpty() }
        assertThat(harness.state.stack.last()).isEqualTo(PaywallNavKey(null))
        (gateway as TestPaymentGateway).withOutcome(ApplicationPack.APPLICATION_PACK_FIFTEEN, PurchaseOutcome.Success)
        composeRule.onNodeWithText("Try again").performClick()

        harness.waitFor { harness.state.stack.last() == CreditsNavKey() }
        assertThat(harness.sheetHost.current).isNull()
    }

    @Test
    fun pendingPurchaseToastsAndStaysOnPaywall() {
        (gateway as TestPaymentGateway).withOutcome(ApplicationPack.APPLICATION_PACK_FIFTEEN, PurchaseOutcome.Pending)
        showWith(CreditsNavKey(), PaywallNavKey(null))

        buy()

        harness.waitFor { harness.toast.current != null }
        assertThat(harness.toast.current?.message).isEqualTo("Payment is pending. Credits arrive when it completes.")
        assertThat(harness.state.stack.last()).isEqualTo(PaywallNavKey(null))
    }
}
