package com.tailormyresume.feature.applications.impl.navigation

import android.app.Application
import android.content.ComponentName
import android.content.Intent
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
import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.NoRemoteLedger
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import com.tailormyresume.core.data.repository.StoredContentReportRepository
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ApplicationsEntryHostActivity : ComponentActivity()

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ApplicationsHiltTestBindings {
    @Binds
    abstract fun bindContentReports(impl: StoredContentReportRepository): ContentReportRepository

    @Binds
    abstract fun bindRemoteLedger(impl: NoRemoteLedger): RemoteLedgerSource

    companion object {
        @Provides
        @Singleton
        fun provideExportedPdfIntents(): ExportedPdfIntents = object : ExportedPdfIntents {
            override suspend fun shareIntent(applicationId: String): Intent? = null

            override suspend fun viewIntent(applicationId: String): Intent? = null
        }
    }
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ApplicationsRealEntryHiltTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<ApplicationsEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, ApplicationsEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var applicationRepository: ApplicationRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var creditsRepository: CreditsRepository

    private val state = NavigationState(NavBackStack<NavKey>(DefaultApplicationsNavKey))
    private val navigator = Navigator(state)
    private val toast = TmrToastState()
    private val provider = entryProvider {
        applicationsEntry(navigator)
        applicationDetailEntry(navigator)
    }

    private fun seed(withCredits: Boolean) {
        hiltRule.inject()
        val scenario = PrototypeFixtures.returning()
        runBlocking {
            profileRepository.saveProfile(scenario.profile)
            scenario.applications.forEach { application ->
                applicationRepository.upsertApplication(
                    if (application.id == "kb") application.copy(status = ApplicationStatus.APPLIED) else application,
                )
            }
            if (withCredits) scenario.ledger.forEach { creditsRepository.record(it) }
        }
    }

    private fun show() {
        composeRule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalTmrToast provides toast) {
                    provider(state.currentKey).Content()
                    TmrToastHost(state = toast)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun stored(id: String): JobApplication =
        runBlocking { checkNotNull(applicationRepository.observeApplication(id).first()) }

    private fun waitFor(condition: () -> Boolean) = composeRule.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idle()
        condition()
    }

    private fun hasText(text: String, substring: Boolean = false) =
        composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun listOpensDetailAndStatusSaveStoresInterview() {
        seed(withCredits = true)
        show()
        waitFor { hasText("Kestrel Bank", substring = true) }

        assertThat(hasText("Recent")).isTrue()
        composeRule.onNodeWithText("Kestrel Bank", substring = true).performClick()
        waitFor { state.currentKey == ApplicationDetailNavKey("kb") && hasText("Mumbai · On-site") }

        composeRule.onNodeWithText("Change").performClick()
        waitFor { hasText("Save status") }
        composeRule.onNodeWithText("Interview").performClick()
        composeRule.onNodeWithText("Save status").performClick()
        waitFor { stored("kb").status == ApplicationStatus.INTERVIEW }

        waitFor { hasText("Status set to Interview") }
    }

    @Test
    fun retailorWithNoCreditsOpensPaywall() {
        seed(withCredits = false)
        navigator.navigate(ApplicationDetailNavKey("kb"))
        show()
        waitFor { hasText("Re-tailor with profile changes") }

        composeRule.onNodeWithText("Re-tailor with profile changes").performClick()

        waitFor { state.currentKey == PaywallNavKey(returnToApplicationId = "kb") }
    }
}
