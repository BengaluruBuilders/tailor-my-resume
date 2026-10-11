package com.tailormyresume.feature.analysis.impl.job

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.ImportedJob
import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.JobDescriptionAnalyzer
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.domain.ProposeJobLabelUseCase
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator
import com.tailormyresume.feature.analysis.impl.joblink.JobLinkViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class JobViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    @Test
    fun emptyStateHasNoTextAndAnalyzeDisabled() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource()
        val applications = TestApplicationRepository()
        val viewModel = viewModel(source = source, applicationRepository = applications)
        val events = collectEvents(viewModel)

        assertThat(viewModel.uiState.value).isEqualTo(JobUiState.Empty)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat(viewModel.uiState.value).isEqualTo(JobUiState.Empty)
        assertThat(source.calls).isEmpty()
        assertThat(applications.observeApplications().first()).isEmpty()
        assertThat(events).isEmpty()
    }

    @Test
    fun detectedLabelShownAtOrAbove200Chars() = runTest(dispatcher) {
        val cases = listOf(
            Triple("Associate Analyst", "Northwind GCC", "Associate Analyst · Northwind GCC"),
            Triple("Associate Analyst", "", "Associate Analyst"),
            Triple("", "", null),
        )

        cases.forEach { (role, org, expected) ->
            val analyzer = FakeJobDescriptionAnalyzer(title = { role }, company = { org })
            val viewModel = viewModel(analyzer = analyzer)

            viewModel.onTextChange(POST_200)
            advanceTimeBy(1_000)

            assertThat(analyzer.proposals).containsExactly(POST_200)
            val state = viewModel.uiState.value as JobUiState.HasText
            assertThat(state.text).isEqualTo(POST_200)
            assertThat(state.detected).isEqualTo(expected)
            assertThat(state.notAJobPost).isFalse()
        }
    }

    @Test
    fun detectedLabelHiddenBelow200() = runTest(dispatcher) {
        val analyzer = FakeJobDescriptionAnalyzer()
        val viewModel = viewModel(analyzer = analyzer)

        viewModel.onTextChange(POST_199)
        advanceTimeBy(1_000)

        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.text).isEqualTo(POST_199)
        assertThat(state.detected).isNull()
        assertThat(analyzer.proposals).isEmpty()
    }

    @Test
    fun labelDetectionRunsOffMainAndLatestWins() = runTest(dispatcher) {
        val offMain = StandardTestDispatcher(testScheduler)
        val analyzer = FakeJobDescriptionAnalyzer(
            title = { text -> if (text == POST_250) "Latest Analyst" else "Earlier Analyst" },
        )
        val viewModel = viewModel(analyzer = analyzer, defaultDispatcher = offMain)
        val events = collectEvents(viewModel)

        viewModel.onTextChange(POST_200)
        viewModel.onTextChange(POST_250)
        advanceTimeBy(1_000)

        assertThat(analyzer.proposals).doesNotContain(POST_200)
        assertThat(analyzer.proposals.last()).isEqualTo(POST_250)
        assertThat(analyzer.interceptors).isNotEmpty()
        assertThat(analyzer.interceptors.first()).isSameInstanceAs(offMain)
        assertThat(analyzer.interceptors.first()).isNotSameInstanceAs(Dispatchers.Main)
        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.text).isEqualTo(POST_250)
        assertThat(state.detected).isEqualTo("Latest Analyst · Northwind GCC")
        assertThat(events).isEmpty()
    }

    @Test
    fun pasteFillsTextAndClearsError() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onTextChange(POST_120)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)
        assertThat((viewModel.uiState.value as JobUiState.HasText).notAJobPost).isTrue()

        viewModel.onPaste(POST_250)

        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.text).isEqualTo(POST_250)
        assertThat(state.notAJobPost).isFalse()
    }

    @Test
    fun clearEmptiesTextAndHidesError() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onTextChange(POST_120)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)
        assertThat((viewModel.uiState.value as JobUiState.HasText).notAJobPost).isTrue()

        viewModel.onClear()

        assertThat(viewModel.uiState.value).isEqualTo(JobUiState.Empty)
    }

    @Test
    fun typingClearsError() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onTextChange(POST_120)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)
        assertThat((viewModel.uiState.value as JobUiState.HasText).notAJobPost).isTrue()

        viewModel.onTextChange("x")
        advanceTimeBy(1_000)

        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.text).isEqualTo("x")
        assertThat(state.notAJobPost).isFalse()
        assertThat(state.detected).isNull()
    }

    @Test
    fun under200TrimmedShowsNotAJobPostWithoutAnalysisCall() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource()
        val applications = TestApplicationRepository()
        val viewModel = viewModel(source = source, applicationRepository = applications)
        val events = collectEvents(viewModel)

        viewModel.onTextChange(POST_120)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.text).isEqualTo(POST_120)
        assertThat(state.notAJobPost).isTrue()
        assertThat(source.calls).isEmpty()
        assertThat(applications.observeApplications().first()).isEmpty()
        assertThat(events).isEmpty()
    }

    @Test
    fun exactly199AndExactly200Boundary() = runTest(dispatcher) {
        val shortSource = FakeJobAnalysisSource()
        val shortViewModel = viewModel(source = shortSource)

        shortViewModel.onTextChange(POST_199)
        shortViewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat((shortViewModel.uiState.value as JobUiState.HasText).notAJobPost).isTrue()
        assertThat(shortSource.calls).isEmpty()

        val longSource = FakeJobAnalysisSource()
        val longViewModel = viewModel(source = longSource)

        longViewModel.onTextChange(POST_200)
        longViewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat(longSource.calls).containsExactly(POST_200)
        assertThat((longViewModel.uiState.value as? JobUiState.HasText)?.notAJobPost ?: false).isFalse()
    }

    @Test
    fun whitespaceOnlyPaddingDoesNotCount() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource()
        val padded = " ".repeat(75) + POST_150 + " ".repeat(75)
        assertThat(padded.length).isEqualTo(300)
        val viewModel = viewModel(source = source)

        viewModel.onTextChange(padded)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.notAJobPost).isTrue()
        assertThat(source.calls).isEmpty()
    }

    @Test
    fun inputCappedAtMax() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onTextChange(postOf(MAX_JOB_CHARS + 500))

        assertThat(viewModel.uiState.value.text.length).isEqualTo(MAX_JOB_CHARS)

        viewModel.onPaste(postOf(MAX_JOB_CHARS + 500))

        assertThat(viewModel.uiState.value.text.length).isEqualTo(MAX_JOB_CHARS)
    }

    @Test
    fun backendNotAJobPostShowsSameStateAndCreatesNothing() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource(failure = AiException(AiFailure.NotAJobPost))
        val applications = TestApplicationRepository()
        val viewModel = viewModel(source = source, applicationRepository = applications)
        val events = collectEvents(viewModel)

        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        val state = viewModel.uiState.value as JobUiState.HasText
        assertThat(state.text).isEqualTo(POST_250)
        assertThat(state.notAJobPost).isTrue()
        assertThat(applications.observeApplications().first()).isEmpty()
        assertThat(events).isEmpty()
    }

    @Test
    fun analyzingPercentAdvancesOnVirtualTimeAndCapsAt95() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val source = FakeJobAnalysisSource().apply { this.gate = gate }
        val viewModel = viewModel(source = source, progressTicker = DelayAnalysisProgressTicker())
        val states = collectStates(viewModel)

        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        runCurrent()

        val started = viewModel.uiState.value as JobUiState.Analyzing
        assertThat(started.text).isEqualTo(POST_250)
        assertThat(started.percent).isAtMost(95)

        advanceTimeBy(1_000)
        assertThat((viewModel.uiState.value as JobUiState.Analyzing).percent).isEqualTo(25)

        advanceTimeBy(10_000)
        assertThat((viewModel.uiState.value as JobUiState.Analyzing).percent).isEqualTo(95)
        assertThat(states.filterIsInstance<JobUiState.Analyzing>()).isNotEmpty()
        states.filterIsInstance<JobUiState.Analyzing>().forEach { assertThat(it.percent).isAtMost(95) }

        gate.complete(Unit)
        runCurrent()
    }

    @Test
    fun successCreatesApplicationWithCoverageAndNavigates() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource()
        val applications = TestApplicationRepository()
        val draftStore = JobDraftStore(TestSessionRepository(), backgroundScope)
        val clock = TestClock()
        val viewModel = viewModel(
            source = source,
            applicationRepository = applications,
            draftStore = draftStore,
            clock = clock,
        )
        val events = collectEvents(viewModel)

        draftStore.set(text = POST_250, importedFrom = "careers.northwind.example")
        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        val saved = applications.observeApplications().first().single()
        assertThat(saved.id).isEqualTo(TestIdGenerator("app").newId())
        assertThat(saved.status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(saved.tailoredResume).isNull()
        assertThat(saved.job).isEqualTo(RESULT.job)
        assertThat(saved.gapAnalysis).isEqualTo(RESULT.gap)
        assertThat(saved.location).isEqualTo(RESULT.job.location.orEmpty())
        assertThat(saved.keywordCoverage).isEqualTo(KeywordCoverageCalculator.compute(RESULT.gap.matches, null, null))
        assertThat(saved.createdAt).isEqualTo(clock.instant)
        assertThat(saved.updatedAt).isEqualTo(clock.instant)
        assertThat(events.filterIsInstance<JobEvent.Analyzed>()).containsExactly(JobEvent.Analyzed(saved.id))
        assertThat(draftStore.draft.value).isNull()
        assertThat(viewModel.uiState.value).isEqualTo(JobUiState.Empty)
    }

    @Test
    fun noCreditOrTailorCallOnAnalyze() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource()
        val applications = TestApplicationRepository()
        val viewModel = viewModel(source = source, applicationRepository = applications)
        collectEvents(viewModel)

        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat(source.calls).containsExactly(POST_250)
        val saved = applications.observeApplications().first().single()
        assertThat(saved.tailoredResume).isNull()
    }

    @Test
    fun otherFailureEmitsRetryToastAndKeepsText() = runTest(dispatcher) {
        val failures = listOf(
            AiException(AiFailure.Network),
            AiException(AiFailure.Timeout),
            AiException(AiFailure.RateLimited),
            AiException(AiFailure.AllowanceExhausted),
            RuntimeException("boom"),
        )

        failures.forEach { failure ->
            val source = FakeJobAnalysisSource(failure = failure)
            val applications = TestApplicationRepository()
            val viewModel = viewModel(source = source, applicationRepository = applications)
            val events = collectEvents(viewModel)

            viewModel.onTextChange(POST_250)
            viewModel.onAnalyze()
            advanceTimeBy(1_000)

            assertThat(events).contains(JobEvent.AnalysisFailed)
            val state = viewModel.uiState.value as JobUiState.HasText
            assertThat(state.text).isEqualTo(POST_250)
            assertThat(state.notAJobPost).isFalse()
            assertThat(applications.observeApplications().first()).isEmpty()
        }
    }

    @Test
    fun missingProfileIsTreatedAsFailure() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource()
        val applications = TestApplicationRepository()
        val viewModel = viewModel(
            source = source,
            applicationRepository = applications,
            saveProfile = null,
        )
        val events = collectEvents(viewModel)

        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat(events).contains(JobEvent.AnalysisFailed)
        assertThat((viewModel.uiState.value as JobUiState.HasText).text).isEqualTo(POST_250)
        assertThat(source.calls).isEmpty()
        assertThat(applications.observeApplications().first()).isEmpty()
    }

    @Test
    fun retryRerunsAnalysis() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource(failure = AiException(AiFailure.Network))
        val applications = TestApplicationRepository()
        val viewModel = viewModel(source = source, applicationRepository = applications)
        val events = collectEvents(viewModel)

        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)
        assertThat(events).contains(JobEvent.AnalysisFailed)

        source.failure = null
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat(events.filterIsInstance<JobEvent.Analyzed>()).hasSize(1)
        assertThat(applications.observeApplications().first()).hasSize(1)
        assertThat(source.calls).containsExactly(POST_250, POST_250)
    }

    @Test
    fun cancellationNotSwallowed() = runTest(dispatcher) {
        val source = FakeJobAnalysisSource(failure = kotlinx.coroutines.CancellationException("cancelled"))
        val applications = TestApplicationRepository()
        val viewModel = viewModel(source = source, applicationRepository = applications)
        val events = collectEvents(viewModel)
        val states = collectStates(viewModel)

        viewModel.onTextChange(POST_250)
        viewModel.onAnalyze()
        advanceTimeBy(1_000)

        assertThat(events.filterIsInstance<JobEvent.AnalysisFailed>()).isEmpty()
        assertThat(states.filterIsInstance<JobUiState.HasText>().map { it.notAJobPost }).doesNotContain(true)
        assertThat(applications.observeApplications().first()).isEmpty()
    }

    @Test
    fun importedDraftFillsTextAndShowsImportedToastOnce() = runTest(dispatcher) {
        val draftStore = JobDraftStore(TestSessionRepository(), backgroundScope)
        val viewModel = viewModel(draftStore = draftStore)
        val events = collectEvents(viewModel)

        draftStore.set(text = POST_250, importedFrom = "careers.northwind.example")
        advanceTimeBy(1_000)

        assertThat((viewModel.uiState.value as JobUiState.HasText).text).isEqualTo(POST_250)
        assertThat(events.filterIsInstance<JobEvent.Imported>()).containsExactly(JobEvent.Imported("careers.northwind.example"))
        assertThat(draftStore.draft.value).isNull()
    }

    @Test
    fun importedFailureShowsNotAJobPost() = runTest(dispatcher) {
        val draftStore = JobDraftStore(TestSessionRepository(), backgroundScope)
        val viewModel = viewModel(draftStore = draftStore)
        val events = collectEvents(viewModel)

        draftStore.markNotAJobPost()
        advanceTimeBy(1_000)

        assertThat(viewModel.uiState.value).isEqualTo(JobUiState.HasText(text = "", notAJobPost = true))
        assertThat(draftStore.draft.value).isNull()
        assertThat(events.filterIsInstance<JobEvent.Imported>()).isEmpty()
    }

    @Test
    fun everyFailedLinkImportLandsOnBlankNotAJobPostState() = runTest(dispatcher) {
        val importFailures: List<Pair<String, Throwable?>> = listOf(
            "https://careers.example/a" to AiException(AiFailure.JobImportFailed),
            "https://careers.example/a" to AiException(AiFailure.RateLimited, 30),
            "https://careers.example/a" to RuntimeException("boom"),
            "http://careers.example/a" to null,
            "   " to null,
        )

        importFailures.forEach { (link, failure) ->
            val draftStore = JobDraftStore(TestSessionRepository(), backgroundScope)
            val importer = object : JobImporter {
                override suspend fun import(url: String): ImportedJob = throw failure ?: AssertionError("importer called")
            }
            val viewModel = viewModel(draftStore = draftStore)

            JobLinkViewModel(importer, draftStore).apply {
                onLinkChange(link)
                onImport()
            }
            advanceTimeBy(1_000)

            assertThat(viewModel.uiState.value).isEqualTo(JobUiState.HasText(text = "", notAJobPost = true))
        }
    }

    private suspend fun TestScope.viewModel(
        source: FakeJobAnalysisSource = FakeJobAnalysisSource(),
        analyzer: FakeJobDescriptionAnalyzer = FakeJobDescriptionAnalyzer(),
        applicationRepository: TestApplicationRepository = TestApplicationRepository(),
        profileRepository: TestProfileRepository = TestProfileRepository(),
        draftStore: JobDraftStore = JobDraftStore(TestSessionRepository(), backgroundScope),
        progressTicker: AnalysisProgressTicker = DelayAnalysisProgressTicker(),
        idGenerator: TestIdGenerator = TestIdGenerator("app"),
        clock: TestClock = TestClock(),
        defaultDispatcher: CoroutineDispatcher = dispatcher,
        saveProfile: CandidateProfile? = PrototypeFixtures.returning().profile,
    ): JobViewModel {
        saveProfile?.let { profileRepository.saveProfile(it) }
        return JobViewModel(
            analyzeJob = AnalyzeJobUseCase(source),
            proposeJobLabel = ProposeJobLabelUseCase(analyzer),
            applicationRepository = applicationRepository,
            profileRepository = profileRepository,
            draftStore = draftStore,
            progressTicker = progressTicker,
            idGenerator = idGenerator,
            clock = clock,
            defaultDispatcher = defaultDispatcher,
        )
    }

    private fun TestScope.collectEvents(viewModel: JobViewModel): List<JobEvent> {
        val events = mutableListOf<JobEvent>()
        backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }
        return events
    }

    private fun TestScope.collectStates(viewModel: JobViewModel): List<JobUiState> {
        val states = mutableListOf<JobUiState>()
        backgroundScope.launch(dispatcher) { viewModel.uiState.toList(states) }
        return states
    }

    private class FakeJobAnalysisSource(
        var result: JobAnalysisResult = RESULT,
        var failure: Throwable? = null,
    ) : JobAnalysisSource {
        val calls = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun analyse(profile: CandidateProfile, rawJobText: String): JobAnalysisResult {
            calls += rawJobText
            gate?.await()
            failure?.let { throw it }
            return result
        }
    }

    private class FakeJobDescriptionAnalyzer(
        private val title: (String) -> String = { "Associate Analyst" },
        private val company: (String) -> String = { "Northwind GCC" },
    ) : JobDescriptionAnalyzer {
        val proposals = mutableListOf<String>()
        val interceptors = mutableListOf<ContinuationInterceptor?>()

        override suspend fun analyze(rawText: String): JobDescription {
            proposals += rawText
            interceptors += coroutineContext[ContinuationInterceptor]
            return JobDescription(
                title = title(rawText),
                company = company(rawText),
                rawText = rawText,
                requirements = emptyList(),
            )
        }
    }

    private companion object {

        const val HEAD = "Associate Analyst, Northwind GCC. Bengaluru, hybrid, full-time. "
        const val FILLER = "You will build dashboards, write SQL and present findings to stakeholders. "

        fun postOf(length: Int): String {
            val text = buildString {
                append(HEAD)
                while (this.length < length) append(FILLER)
            }.take(length)
            return text.replace(Regex("\\s+$"), "x")
        }

        val POST_120 = postOf(120)
        val POST_150 = postOf(150)
        val POST_199 = postOf(199)
        val POST_200 = postOf(200)
        val POST_250 = postOf(250)

        val REQUIREMENT = JobRequirement(
            id = "req-sql",
            text = "Strong SQL and Power BI",
            type = RequirementType.TOOL,
            priority = RequirementPriority.MUST_HAVE,
            keywords = listOf("SQL", "Power BI"),
        )

        val RESULT = JobAnalysisResult(
            job = JobDescription(
                title = "Associate Analyst",
                company = "Northwind GCC",
                rawText = POST_250,
                requirements = listOf(REQUIREMENT),
                location = "Bengaluru · Hybrid",
            ),
            gap = GapAnalysis(
                matches = listOf(
                    RequirementMatch(
                        requirement = REQUIREMENT,
                        status = MatchStatus.MET,
                        evidenceIds = listOf("exp-infosys-b1"),
                    ),
                ),
                keywordCoverage = KeywordCoverage(covered = 1, total = 2),
            ),
        )
    }
}
