package com.tailormyresume.feature.analysis.impl.joblink

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.ImportedJob
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.analysis.impl.job.JobDraft
import com.tailormyresume.feature.analysis.impl.job.JobDraftStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobLinkViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    @Test
    fun initialStateEmptyLinkEnablesImport() = runTest(dispatcher) {
        val importer = FakeJobImporter()
        val viewModel = viewModel(importer)

        assertThat(viewModel.uiState.value).isEqualTo(JobLinkUiState(link = "", importing = false))

        viewModel.onLinkChange("https://x.example/a")

        assertThat(viewModel.uiState.value.link).isEqualTo("https://x.example/a")
        assertThat(viewModel.uiState.value.importing).isFalse()
    }

    @Test
    fun httpsImportReturnsTextAndHostThenPops() = runTest(dispatcher) {
        listOf(URL, "  $URL  ", "\t$URL\n").forEach { typed ->
            val importer = FakeJobImporter()
            val store = store()
            val viewModel = viewModel(importer, store)
            val events = collectEvents(viewModel.events)

            viewModel.onLinkChange(typed)
            viewModel.onImport()
            runCurrent()

            assertThat(store.draft.value).isEqualTo(JobDraft(text = JOB_TEXT, importedFrom = HOST))
            assertThat(events).containsExactly(JobLinkEvent.Close)
            assertThat(viewModel.uiState.value.importing).isFalse()
            assertThat(importer.urls).containsExactly(URL)
        }
    }

    @Test
    fun importInFlightShowsImportingAndIgnoresSecondTap() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val importer = FakeJobImporter().apply { this.gate = gate }
        val store = store()
        val viewModel = viewModel(importer, store)
        val events = collectEvents(viewModel.events)

        viewModel.onLinkChange(URL)
        viewModel.onImport()
        runCurrent()

        assertThat(viewModel.uiState.value.importing).isTrue()
        assertThat(store.draft.value).isNull()
        assertThat(events).isEmpty()

        viewModel.onImport()
        runCurrent()

        assertThat(importer.urls).containsExactly(URL)
        assertThat(store.draft.value).isNull()
        assertThat(events).isEmpty()

        gate.complete(Unit)
        runCurrent()

        assertThat(store.draft.value).isEqualTo(JobDraft(text = JOB_TEXT, importedFrom = HOST))
        assertThat(events).containsExactly(JobLinkEvent.Close)
        assertThat(viewModel.uiState.value.importing).isFalse()
    }

    @Test
    fun httpLinkReturnsNotAJobPostWithoutCallingImporter() = runTest(dispatcher) {
        val cases = listOf("http://careers.example/a", "", "   ", "ftp://x")

        cases.forEach { typed ->
            val importer = FakeJobImporter()
            val store = store()
            val viewModel = viewModel(importer, store)
            val events = collectEvents(viewModel.events)

            viewModel.onLinkChange(typed)
            viewModel.onImport()
            runCurrent()

            assertThat(store.draft.value).isEqualTo(JobDraft(notAJobPost = true))
            assertThat(events).containsExactly(JobLinkEvent.Close)
            assertThat(viewModel.uiState.value.importing).isFalse()
            assertThat(importer.urls).isEmpty()
        }
    }

    @Test
    fun importFailedReturnsNotAJobPost() = runTest(dispatcher) {
        val importer = FakeJobImporter(failure = AiException(AiFailure.JobImportFailed))
        val store = store()
        val viewModel = viewModel(importer, store)
        val events = collectEvents(viewModel.events)

        viewModel.onLinkChange(URL)
        viewModel.onImport()
        runCurrent()

        assertThat(store.draft.value).isEqualTo(JobDraft(notAJobPost = true))
        assertThat(events).containsExactly(JobLinkEvent.Close)
        assertThat(viewModel.uiState.value.importing).isFalse()
        assertThat(importer.urls).containsExactly(URL)
    }

    @Test
    fun rateLimitedAndAllowanceAlsoReturnNotAJobPost() = runTest(dispatcher) {
        val failures = listOf(
            AiException(AiFailure.RateLimited, 30),
            AiException(AiFailure.AllowanceExhausted),
            AiException(AiFailure.NotAJobPost),
            AiException(AiFailure.Network),
            RuntimeException("boom"),
        )

        failures.forEach { failure ->
            val importer = FakeJobImporter(failure = failure)
            val store = store()
            val viewModel = viewModel(importer, store)
            val events = collectEvents(viewModel.events)

            viewModel.onLinkChange(URL)
            viewModel.onImport()
            runCurrent()

            assertThat(store.draft.value).isEqualTo(JobDraft(notAJobPost = true))
            assertThat(events).containsExactly(JobLinkEvent.Close)
            assertThat(viewModel.uiState.value.importing).isFalse()
            assertThat(importer.urls).containsExactly(URL)
        }
    }

    @Test
    fun tooLongLink() = runTest(dispatcher) {
        val tooLongImporter = FakeJobImporter()
        val tooLongStore = store()
        val tooLongViewModel = viewModel(tooLongImporter, tooLongStore)
        val tooLongEvents = collectEvents(tooLongViewModel.events)

        tooLongViewModel.onLinkChange(httpsLinkOf(MAX_LINK_CHARS + 1))
        tooLongViewModel.onImport()
        runCurrent()

        assertThat(tooLongStore.draft.value).isEqualTo(JobDraft(notAJobPost = true))
        assertThat(tooLongEvents).containsExactly(JobLinkEvent.Close)
        assertThat(tooLongImporter.urls).isEmpty()

        val exactImporter = FakeJobImporter()
        val exactStore = store()
        val exactViewModel = viewModel(exactImporter, exactStore)
        val exactEvents = collectEvents(exactViewModel.events)
        val exactLink = httpsLinkOf(MAX_LINK_CHARS)

        exactViewModel.onLinkChange(exactLink)
        exactViewModel.onImport()
        runCurrent()

        assertThat(exactImporter.urls).containsExactly(exactLink)
        assertThat(exactStore.draft.value).isEqualTo(JobDraft(text = JOB_TEXT, importedFrom = HOST))
        assertThat(exactEvents).containsExactly(JobLinkEvent.Close)
        assertThat(exactViewModel.uiState.value.importing).isFalse()
    }

    @Test
    fun cancellationIsNotReportedAsFailure() = runTest(dispatcher) {
        val importer = FakeJobImporter(failure = CancellationException("cancelled"))
        val store = store()
        val viewModel = viewModel(importer, store)
        val events = collectEvents(viewModel.events)

        viewModel.onLinkChange(URL)
        viewModel.onImport()
        runCurrent()

        assertThat(store.draft.value).isNull()
        assertThat(events).isEmpty()
        assertThat(importer.urls).containsExactly(URL)
    }

    private fun TestScope.store(): JobDraftStore = JobDraftStore(TestSessionRepository(), backgroundScope)

    private fun TestScope.viewModel(
        importer: FakeJobImporter,
        draftStore: JobDraftStore = store(),
    ): JobLinkViewModel = JobLinkViewModel(importer = importer, draftStore = draftStore)

    private fun TestScope.collectEvents(flow: Flow<JobLinkEvent>): List<JobLinkEvent> {
        val events = mutableListOf<JobLinkEvent>()
        backgroundScope.launch(dispatcher) { flow.toList(events) }
        return events
    }

    private class FakeJobImporter(
        private val result: ImportedJob = ImportedJob(jobText = JOB_TEXT, sourceHost = HOST),
        private val failure: Throwable? = null,
    ) : JobImporter {
        val urls = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun import(url: String): ImportedJob {
            urls += url
            gate?.await()
            failure?.let { throw it }
            return result
        }
    }

    private companion object {

        const val HOST = "careers.northwind.example"
        const val URL = "https://careers.northwind.example/associate-analyst"
        const val JOB_TEXT = "Associate Analyst, Northwind GCC. Bengaluru, hybrid, full-time."
        const val MAX_LINK_CHARS = 2048

        fun httpsLinkOf(length: Int): String {
            val prefix = "https://x.example/"
            return prefix + "a".repeat(length - prefix.length)
        }
    }
}
