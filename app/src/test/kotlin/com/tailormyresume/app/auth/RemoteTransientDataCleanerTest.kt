package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ai.ANALYSIS_RESPONSE
import com.tailormyresume.app.ai.FakeBackend
import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.ai.candidate
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import kotlin.time.Instant

class RemoteTransientDataCleanerTest {
    private val backend = FakeBackend()
    private val analysis = RemoteJobAnalysisSource(backend.api, NoMatcher)
    private val reports = PendingReportQueue(TestMockStateStore())
    private val cleaner = RemoteTransientDataCleaner(analysis, reports)

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun clearsTheAnalysisCacheAndThePendingReportsWithoutANetworkCall() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        analysis.analyse(candidate, "the raw job text")
        reports.add(ContentReport("app-1", ReportedItemKind.REQUIREMENT, "req-1", "text", Instant.fromEpochMilliseconds(1), null))
        val requestsBefore = backend.server.requestCount

        cleaner.clear()

        assertThat(backend.server.requestCount).isEqualTo(requestsBefore)
        assertThat(reports.pending()).isEmpty()
        backend.reply(200, ANALYSIS_RESPONSE)
        analysis.analyse(candidate, "the raw job text")
        assertThat(backend.server.requestCount).isEqualTo(requestsBefore + 1)
    }
}
