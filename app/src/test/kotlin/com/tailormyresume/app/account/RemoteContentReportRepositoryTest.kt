package com.tailormyresume.app.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.data.repository.StoredContentReportRepository
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import kotlin.time.Instant

class RemoteContentReportRepositoryTest {
    private val server = MockWebServer().apply { start() }
    private val store = TestMockStateStore()
    private val queue = PendingReportQueue(store)
    private val repository = RemoteContentReportRepository(
        StoredContentReportRepository(store),
        queue,
        server.api(),
        CoroutineScope(StandardTestDispatcher()),
    )
    private val report = ContentReport("app-1", ReportedItemKind.RESUME_BULLET, "b1", "Led a team", Instant.fromEpochMilliseconds(5))

    @After
    fun tearDown() = server.shutdown()

    private val created = """{"report":{"id":"r1","reportedAt":"2026-10-08T00:00:00Z"}}"""

    @Test
    fun createdReportIsPostedWithNullGenerationIdAndLeavesTheQueue() = runTest {
        server.enqueue(jsonResponse(201, created))

        repository.report(report)
        repository.flush()

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/tailormyresume/content-reports")
        assertThat(request.body.readUtf8()).contains("\"generationId\":null")
        assertThat(queue.pending()).isEmpty()
        assertThat(repository.observeReportedIdsNow()).containsExactly("b1")
    }

    @Test
    fun repeatedReportAnswered200LeavesTheQueue() = runTest {
        server.enqueue(jsonResponse(200, created))

        repository.report(report)
        repository.flush()

        assertThat(queue.pending()).isEmpty()
    }

    @Test
    fun failedPostStaysQueuedAndIsRetriedOnTheNextFlush() = runTest {
        server.enqueue(errorResponse(500, "INTERNAL_ERROR"))
        server.enqueue(jsonResponse(201, created))

        repository.report(report)
        repository.flush()
        assertThat(queue.pending()).containsExactly(report)
        assertThat(repository.observeReportedIdsNow()).containsExactly("b1")

        repository.flush()
        assertThat(queue.pending()).isEmpty()
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun rejectedReportIsDroppedBecauseARetryCannotSucceed() = runTest {
        server.enqueue(errorResponse(400, "INVALID_INPUT"))

        repository.report(report)
        repository.flush()

        assertThat(queue.pending()).isEmpty()
    }

    private suspend fun RemoteContentReportRepository.observeReportedIdsNow() =
        observeReportedIds("app-1", ReportedItemKind.RESUME_BULLET).first()
}
