package com.tailormyresume.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class PendingReportQueueTest {

    private val queue = PendingReportQueue(TestMockStateStore())

    private fun report(applicationId: String = "app-1", itemId: String = "req-1", generationId: String? = "gen-1") =
        ContentReport(applicationId, ReportedItemKind.REQUIREMENT, itemId, "text", Instant.fromEpochMilliseconds(1), generationId)

    @Test
    fun destinationCannotFlushSourceReportsEvenBeforeGenericCleanup() = runTest {
        queue.add(report(), owner = "A")
        queue.add(report(), owner = "B")
        assertThat(queue.pending("C")).isEmpty()
        assertThat(queue.pending("B")).containsExactly(report())
        queue.remove(report(), "B")
        assertThat(queue.pending("A")).containsExactly(report())
        assertThat(queue.pending("B")).isEmpty()
    }

    @Test
    fun aReportIsQueuedOnceAndKeepsItsGenerationId() = runTest {
        queue.add(report())
        queue.add(report())

        assertThat(queue.pending()).containsExactly(report())
    }

    @Test
    fun removeDropsOnlyThatItem() = runTest {
        queue.add(report(itemId = "req-1"))
        queue.add(report(itemId = "req-2"))

        queue.remove(report(itemId = "req-1"))

        assertThat(queue.pending().map { it.itemId }).containsExactly("req-2")
    }

    @Test
    fun clearDropsEveryReport() = runTest {
        queue.add(report(applicationId = "app-1"))
        queue.add(report(applicationId = "app-2"))

        queue.clear()

        assertThat(queue.pending()).isEmpty()
    }

    @Test
    fun clearForDropsEveryReportOfOneApplication() = runTest {
        queue.add(report(applicationId = "app-1"))
        queue.add(report(applicationId = "app-2"))

        queue.clearFor("app-1")

        assertThat(queue.pending().map { it.applicationId }).containsExactly("app-2")
    }
}
