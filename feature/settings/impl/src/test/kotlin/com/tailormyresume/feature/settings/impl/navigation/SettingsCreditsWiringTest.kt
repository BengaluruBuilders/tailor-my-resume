package com.tailormyresume.feature.settings.impl.navigation

import android.app.Application
import android.content.ComponentName
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.ReferralsNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
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
import kotlin.time.Instant

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SettingsCreditsWiringTest {

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
    lateinit var credits: CreditsRepository

    private lateinit var harness: EntryHarness

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            credits.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-02T09:00:00Z")))
        }
        harness = EntryHarness(composeRule, listOf(SettingsNavKey(), CreditsNavKey()))
        harness.show()
    }

    @Test
    fun creditsEntryShowsTheLedgerFromTheRepository() {
        harness.waitFor { composeRule.onAllNodesWithText("Free first resume").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("Invite friends").assertExists()
    }

    @Test
    fun inviteOpensReferrals() {
        harness.waitFor { composeRule.onAllNodesWithText("Invite friends").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("Invite friends").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(harness.state.stack.last()).isEqualTo(ReferralsNavKey)
    }
}
