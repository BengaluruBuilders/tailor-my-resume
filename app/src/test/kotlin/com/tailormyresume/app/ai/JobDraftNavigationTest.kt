package com.tailormyresume.app.ai

import android.app.Application
import android.content.ComponentName
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.di.AiBindings
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.ImportedJob
import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.toEntries
import com.tailormyresume.core.network.dto.AnalysisRequest
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.impl.navigation.analysisEntry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

@AndroidEntryPoint
class JobDraftHostActivity : ComponentActivity()

@HiltAndroidTest
@UninstallModules(AiBindings::class)
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class JobDraftNavigationTest {
    private val original = "  Data Analyst at Acme. Responsibilities: Analyze sales data using SQL and Excel. " +
        "Build detailed weekly sales reports for business teams. Requirements: Experience with data cleaning, SQL, Excel, communication and stakeholders.\n  "
    private val backend = FakeBackend()
    private val preview = DebugPreviewMode()
    private val matcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) = GapAnalysis(emptyList(), KeywordCoverage(0, 0))
    }

    @BindValue
    @JvmField
    val source: JobAnalysisSource = GuardedJobAnalysisSource(
        RemoteJobAnalysisSource(backend.api, matcher),
        OfflineJobAnalysisSource(OfflineJobDescriptionAnalyzer(), matcher),
        preview,
    )

    @BindValue
    @JvmField
    val importer: JobImporter = object : JobImporter {
        override suspend fun import(url: String) = ImportedJob("Analyze SQL reports. ".repeat(15), "jobs.example")
    }

    private val hiltRule = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<JobDraftHostActivity>()
    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, JobDraftHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(compose)

    @Inject
    lateinit var profiles: ProfileRepository

    private val state = NavigationState(NavBackStack<NavKey>(JobNavKey()))
    private val navigator = Navigator(state)

    @Before
    fun seed() {
        hiltRule.inject()
        runBlocking { profiles.saveProfile(candidate) }
    }

    @After
    fun close() = backend.shutdown()

    @Test
    fun remoteAnalysisBackKeepsDraftAndSendsEditedText() {
        backend.reply(200, ANALYSIS_RESPONSE)
        backend.reply(200, ANALYSIS_RESPONSE)
        verifyRoundTrip()

        assertThat(backend.server.requestCount).isEqualTo(2)
        val first = backend.server.takeRequest()
        assertThat(first.path).isEqualTo("/v1/tailormyresume/analyses")
        assertThat(tailormyresumeJson().decodeFromString<AnalysisRequest>(first.body.readUtf8()).jobText).isEqualTo(original)
        val edited = backend.server.takeRequest()
        assertThat(tailormyresumeJson().decodeFromString<AnalysisRequest>(edited.body.readUtf8()).jobText)
            .isEqualTo("$original Build Power BI dashboards.")
    }

    @Test
    fun isolatedPreviewAnalysisBackKeepsDraftWithoutNetwork() {
        preview.active = true
        verifyRoundTrip()

        assertThat(backend.server.requestCount).isEqualTo(0)
    }

    @Test
    fun importedLinkSurvivesAnalysisBack() {
        preview.active = true
        showJob()
        compose.onNodeWithText("Use a link").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("https://jobs.example/a")
        compose.onNodeWithText("Import job").performClick()
        compose.onNodeWithText("Analyze job").performClick()
        awaitResult()
        androidBack()

        assertDraft("Analyze SQL reports. ".repeat(15))
        compose.onNodeWithText("Use a link").performClick()
        assertDraft("https://jobs.example/a")
    }

    private fun showJob() {
        val provider = entryProvider<NavKey> { analysisEntry(navigator) }
        compose.setContent {
            TmrTheme {
                NavDisplay(
                    entries = state.toEntries { key ->
                        if (key is JobResultNavKey) NavEntry(key) { Text("Analysis result") } else provider(key)
                    },
                    onBack = { navigator.goBack() },
                )
            }
        }
    }

    private fun verifyRoundTrip() {
        showJob()
        compose.onNode(hasSetTextAction()).performTextInput(original)
        compose.onNodeWithText("Use a link").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("https://jobs.example/a")
        compose.onNodeWithText("Paste text").performClick()
        compose.onNodeWithText("Analyze job").performClick()
        awaitResult()
        androidBack()
        assertDraft(original)
        compose.onNodeWithText("Paste text").assertIsSelected()
        compose.onNodeWithText("Use a link").performClick()
        assertDraft("https://jobs.example/a")
        compose.onNodeWithText("Use a link").assertIsSelected()
        compose.onNodeWithText("Paste text").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("$original Build Power BI dashboards.")
        compose.onNodeWithText("Analyze job").performClick()
        awaitResult()
        androidBack()
        assertDraft("$original Build Power BI dashboards.")
        compose.onNodeWithText("Clear").performClick()
        assertDraft("")
        compose.onNodeWithText("Use a link").performClick()
        assertDraft("")
    }

    private fun assertDraft(expected: String) {
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected)),
        )
    }

    private fun awaitResult() {
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Analysis result").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun androidBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RemainingAiBindings {
        @Binds
        abstract fun tailor(impl: GuardedResumeTailor): ResumeTailor

        @Binds
        abstract fun parser(impl: RemoteResumeTextParser): ResumeTextParser
    }
}
