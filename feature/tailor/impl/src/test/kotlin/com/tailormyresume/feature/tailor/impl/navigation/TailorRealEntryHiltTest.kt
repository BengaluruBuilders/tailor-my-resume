package com.tailormyresume.feature.tailor.impl.navigation

import android.app.Application
import android.content.ComponentName
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.navigation.ChromeActions
import com.tailormyresume.core.navigation.LocalChromeActions
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
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
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject
import kotlin.time.Instant

@AndroidEntryPoint
class TailorEntryHostActivity : ComponentActivity()

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TailorRealEntryHiltTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<TailorEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, TailorEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var applicationRepository: ApplicationRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    private val state = NavigationState(NavBackStack<NavKey>(TailoredNavKey("nw")))
    private val navigator = Navigator(state)
    private val chromeActions = ChromeActions()
    private val provider = entryProvider { tailorEntry(navigator) }

    @Before
    fun seed() {
        hiltRule.inject()
        val scenario = PrototypeFixtures.returning()
        val bullets = scenario.profile.entries
            .filter { it.id == "exp-infosys" }
            .flatMap { entry ->
                entry.bullets.map { bullet ->
                    TailoredBullet(
                        id = bullet.id,
                        entryId = entry.id,
                        originalText = bullet.text,
                        proposedText = bullet.text,
                        sourceIds = listOf(bullet.id),
                        editTypes = emptyList(),
                        keywordsUsed = emptyList(),
                        violations = emptyList(),
                        decision = BulletDecision.ACCEPTED,
                    )
                }
            }
        val northwind = scenario.applications.single { it.id == "nw" }
        runBlocking {
            profileRepository.saveProfile(scenario.profile)
            applicationRepository.upsertApplication(
                northwind.copy(
                    status = ApplicationStatus.SAVED,
                    appliedOn = null,
                    tailoredResume = TailoredResume(bullets = bullets),
                    changesAcceptedAt = Instant.parse("2026-10-09T09:00:00Z"),
                ),
            )
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

    private fun stored(): JobApplication = runBlocking { checkNotNull(applicationRepository.observeApplication("nw").first()) }

    private fun waitFor(condition: () -> Boolean) = composeRule.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idle()
        condition()
    }

    @Test
    fun editResumeEntryRendersAndSaves() {
        navigator.navigate(EditResumeNavKey("nw"))
        show(EditResumeNavKey("nw"))
        waitFor { composeRule.onAllNodesWithText("Edit resume").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("Edit resume").assertExists()
        assertThat(composeRule.onAllNodesWithContentDescription("Edit bullet").fetchSemanticsNodes()).isNotEmpty()
        composeRule.onNodeWithText("Wrote SQL pipelines over 20M+ rows of transaction data.")
            .performTextReplacement("Wrote SQL pipelines over 25M+ rows.")
        composeRule.waitForIdle()
        composeRule.runOnIdle { checkNotNull(chromeActions.handler).invoke() }
        waitFor { stored().tailoredResume?.bullets?.any { it.proposedText == "Wrote SQL pipelines over 25M+ rows." } == true }

        assertThat(stored().changesAcceptedAt).isNotNull()
        waitFor { state.stack.last() == TailoredNavKey("nw") }
    }

    @Test
    fun exportedEntryRendersAndMarksApplied() {
        navigator.navigate(ExportedNavKey("nw"))
        show(ExportedNavKey("nw"))
        waitFor { composeRule.onAllNodesWithText("Resume exported").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("✓ Mark as Applied").performClick()
        waitFor { stored().status == ApplicationStatus.APPLIED }

        assertThat(stored().appliedOn).isNotNull()
        assertThat(stored().exportFileName).isEqualTo("Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf")
    }
}
