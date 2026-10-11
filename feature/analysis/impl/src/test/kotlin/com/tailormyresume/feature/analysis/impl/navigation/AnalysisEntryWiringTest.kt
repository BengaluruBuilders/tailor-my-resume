package com.tailormyresume.feature.analysis.impl.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.ImportedJob
import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.JobDescriptionAnalyzer
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.domain.ProposeJobLabelUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator
import com.tailormyresume.feature.analysis.api.navigation.JobLinkNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.impl.job.AnalysisProgressTicker
import com.tailormyresume.feature.analysis.impl.job.JobDraftStore
import com.tailormyresume.feature.analysis.impl.job.JobViewModel
import com.tailormyresume.feature.analysis.impl.joblink.JobLinkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private const val LINK = "https://jobs.example/a"
private const val ANALYZE = "Analyze job"

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class AnalysisEntryWiringTest {

    @get:Rule
    val rule = createComposeRule()

    private val store = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val draftStore = JobDraftStore(TestSessionRepository(), scope)
    private val importer = RecordingImporter()
    private val owner = object : ViewModelStoreOwner {
        override val viewModelStore = store
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun jobEntryEnablesAnalyzeOnceTextIsTyped() {
        showEntry(JobNavKey())

        assertThat(analyzeStateDescription()).isNotNull()
        rule.onNode(hasSetTextAction()).performTextInput("x".repeat(250))
        rule.waitForIdle()

        assertThat(analyzeStateDescription()).isNull()
    }

    @Test
    fun jobLinkEntryStartsImportForPastedLink() {
        showEntry(JobLinkNavKey())

        rule.onNode(hasSetTextAction()).performTextInput(LINK)
        rule.onNodeWithText("Import job").performClick()
        rule.waitForIdle()

        assertThat(importer.urls).containsExactly(LINK)
    }

    private fun analyzeStateDescription(): String? =
        rule.onNodeWithText(ANALYZE).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun showEntry(key: NavKey) {
        install(JobViewModel::class.java, jobViewModel())
        install(JobLinkViewModel::class.java, JobLinkViewModel(importer, draftStore))
        val navigator = Navigator(NavigationState(NavBackStack<NavKey>(key)))
        val provider = entryProvider<NavKey> { analysisEntry(navigator) }
        rule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    provider(key).Content()
                }
            }
        }
        rule.waitForIdle()
    }

    private fun <T : ViewModel> install(type: Class<T>, viewModel: T) {
        ViewModelProvider(store, SingleInstanceFactory(viewModel))[type]
    }

    private fun jobViewModel() = JobViewModel(
        analyzeJob = AnalyzeJobUseCase(NeverCalledSource),
        proposeJobLabel = ProposeJobLabelUseCase(BlankAnalyzer),
        applicationRepository = TestApplicationRepository(),
        profileRepository = TestProfileRepository(),
        draftStore = draftStore,
        progressTicker = AnalysisProgressTicker { emptyFlow() },
        idGenerator = TestIdGenerator("app"),
        clock = TestClock(),
        defaultDispatcher = Dispatchers.Unconfined,
    )

    private class SingleInstanceFactory(private val instance: ViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = instance as T
    }

    private class RecordingImporter : JobImporter {
        val urls = mutableListOf<String>()

        override suspend fun import(url: String): ImportedJob {
            urls += url
            return ImportedJob(jobText = "x".repeat(250), sourceHost = "jobs.example")
        }
    }

    private object NeverCalledSource : JobAnalysisSource {
        override suspend fun analyse(profile: CandidateProfile, rawJobText: String): JobAnalysisResult =
            error("analysis is not part of this test")
    }

    private object BlankAnalyzer : JobDescriptionAnalyzer {
        override suspend fun analyze(rawText: String) =
            JobDescription(title = "", company = "", rawText = rawText, requirements = emptyList())
    }
}
