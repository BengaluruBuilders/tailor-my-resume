package com.tailormyresume.feature.analysis.impl.navigation

import android.app.Application
import android.content.ComponentName
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.navigation.ChromeActions
import com.tailormyresume.core.navigation.LocalChromeActions
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.analysis.impl.ResultTestData
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import dagger.hilt.android.AndroidEntryPoint
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
import kotlin.time.Instant

private const val TAILOR = "Tailor my resume"
private const val SKIP = "Skip this"

@AndroidEntryPoint
class AnalysisEntryHostActivity : ComponentActivity()

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class JobResultRealEntryTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<AnalysisEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, AnalysisEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var applicationRepository: ApplicationRepository

    @Inject
    lateinit var creditsRepository: CreditsRepository

    private val state = NavigationState(NavBackStack<NavKey>(JobResultNavKey(ResultTestData.APP_ID)))
    private val navigator = Navigator(state)
    private val chromeActions = ChromeActions()
    private val provider = entryProvider { analysisEntry(navigator) }

    @Before
    fun seed() {
        hiltRule.inject()
        runBlocking {
            applicationRepository.upsertApplication(ResultTestData.application())
            creditsRepository.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 3, null, null, Instant.fromEpochSeconds(0)))
        }
    }

    private fun show(key: NavKey) {
        composeRule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalChromeActions provides chromeActions) { provider(key).Content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun tapTailorOpensQuestion() {
        show(JobResultNavKey(ResultTestData.APP_ID))
        awaitText(TAILOR)

        composeRule.onNodeWithText(TAILOR).performClick()
        composeRule.waitForIdle()

        assertThat(state.stack.last()).isEqualTo(QuickQuestionNavKey(ResultTestData.APP_ID))
    }

    @Test
    fun tapTailorWithNoCreditsOpensPaywall() {
        runBlocking {
            creditsRepository.record(CreditLedgerEntry(CreditLedgerKind.SPEND, -3, null, null, Instant.fromEpochSeconds(1)))
        }
        show(JobResultNavKey(ResultTestData.APP_ID))
        awaitText(TAILOR)

        composeRule.onNodeWithText(TAILOR).performClick()
        composeRule.waitForIdle()

        assertThat(state.stack.last()).isEqualTo(PaywallNavKey(returnToApplicationId = ResultTestData.APP_ID))
    }

    @Test
    fun skipOnQuickQuestionForwardsToTailoring() {
        state.stack.add(QuickQuestionNavKey(ResultTestData.APP_ID))
        show(QuickQuestionNavKey(ResultTestData.APP_ID))
        awaitText(SKIP)

        composeRule.onNodeWithText(SKIP).performClick()
        composeRule.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            state.stack.last() !is QuickQuestionNavKey
        }

        val last = state.stack.last()
        assertThat(last).isInstanceOf(TailoringNavKey::class.java)
        assertThat((last as TailoringNavKey).applicationId).isEqualTo(ResultTestData.APP_ID)
        assertThat(state.stack.any { it is QuickQuestionNavKey }).isFalse()
        assertThat(runBlocking { applicationRepository.observeApplication(ResultTestData.APP_ID).first() }?.quickAnswer).isNull()
    }
}
