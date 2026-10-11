package com.tailormyresume.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.model.testApplication
import com.tailormyresume.core.data.model.testBareApplication
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.model.TailoringReviewState
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstApplicationRepositoryTest {

    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }
    private fun TestScope.newRepository() = OfflineFirstApplicationRepository(
        jobApplicationDao = FakeJobApplicationDao(),
        clock = fixedClock,
        cleanup = ApplicationCleanup {},
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun upsertedApplicationIsObserved() = runTest {
        val repository = newRepository()
        repository.upsertApplication(testApplication)

        assertThat(repository.observeApplication(testApplication.id).first())
            .isEqualTo(testApplication)
    }

    @Test
    fun unknownApplicationIsObservedAsNull() = runTest {
        val repository = newRepository()
        assertThat(repository.observeApplication("missing").first()).isNull()
    }

    @Test
    fun applicationsAreOrderedByMostRecentUpdate() = runTest {
        val repository = newRepository()
        val older = testBareApplication.copy(updatedAt = Instant.fromEpochMilliseconds(1))
        repository.upsertApplication(older)
        repository.upsertApplication(testApplication)

        assertThat(repository.observeApplications().first())
            .containsExactly(testApplication, older).inOrder()
    }

    @Test
    fun observeApplicationEmitsNullForMissingIdEvenWhenOthersExist() = runTest {
        val repository = newRepository()
        repository.upsertApplication(testApplication)

        repository.observeApplication("missing").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeApplicationsEmitsAgainAfterUpdateStatus() = runTest {
        val repository = newRepository()
        val older = testBareApplication.copy(updatedAt = Instant.fromEpochMilliseconds(1))
        repository.upsertApplication(older)
        repository.upsertApplication(testApplication)

        repository.observeApplications().test {
            assertThat(awaitItem().map { it.id }).containsExactly(testApplication.id, older.id).inOrder()

            repository.updateStatus(older.id, ApplicationStatus.INTERVIEW)

            val updated = awaitItem()
            assertThat(updated.map { it.id }).containsExactly(older.id, testApplication.id).inOrder()
            assertThat(updated.first().status).isEqualTo(ApplicationStatus.INTERVIEW)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateStatusChangesStatusAndStampsUpdatedAt() = runTest {
        val repository = newRepository()
        repository.upsertApplication(testApplication)

        repository.updateStatus(testApplication.id, ApplicationStatus.OFFER)

        val saved = repository.observeApplication(testApplication.id).first()
        assertThat(saved?.status).isEqualTo(ApplicationStatus.OFFER)
        assertThat(saved?.updatedAt).isEqualTo(now)
        assertThat(saved?.createdAt).isEqualTo(testApplication.createdAt)
    }

    @Test
    fun updateNotesChangesNotesAndStampsUpdatedAt() = runTest {
        val repository = newRepository()
        repository.upsertApplication(testApplication)

        repository.updateNotes(testApplication.id, "Follow up on Monday")

        val saved = repository.observeApplication(testApplication.id).first()
        assertThat(saved?.notes).isEqualTo("Follow up on Monday")
        assertThat(saved?.updatedAt).isEqualTo(now)
    }

    @Test
    fun deletedApplicationIsGone() = runTest {
        val repository = newRepository()
        repository.upsertApplication(testApplication)

        repository.deleteApplication(testApplication.id)

        assertThat(repository.observeApplications().first()).isEmpty()
    }

    @Test
    fun deletingAnApplicationClearsItsReportsReviewStateAndLegacyKeys() = runTest {
        val store = TestMockStateStore()
        val reports = StoredContentReportRepository(store)
        val reviewState = StoredTailoringReviewStateRepository(store)
        val repository = OfflineFirstApplicationRepository(
            jobApplicationDao = FakeJobApplicationDao(),
            clock = fixedClock,
            cleanup = StoredApplicationCleanup(reports, reviewState, store),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        repository.upsertApplication(testApplication)
        reports.report(ContentReport(testApplication.id, ReportedItemKind.RESUME_BULLET, "b1", "text", now))
        reviewState.recordRegeneration(testApplication.id, "EXPERIENCE")
        reviewState.markEdited(testApplication.id, "b1")
        store.write("coverletter.${testApplication.id}", "letter")
        store.write("prep.plan.${testApplication.id}", "plan")
        store.write("coverletter.other", "keep")

        repository.deleteApplication(testApplication.id)

        assertThat(reports.observeReports(testApplication.id).first()).isEmpty()
        assertThat(reviewState.observe(testApplication.id).first()).isEqualTo(TailoringReviewState())
        assertThat(store.read("coverletter.${testApplication.id}")).isNull()
        assertThat(store.read("prep.plan.${testApplication.id}")).isNull()
        assertThat(store.read("coverletter.other")).isEqualTo("keep")
    }

    @Test
    fun whenTheCleanupFailsTheRowStaysAndARetryClearsRowAndKeys() = runTest {
        val store = TestMockStateStore()
        val storedCleanup = StoredApplicationCleanup(
            StoredContentReportRepository(store),
            StoredTailoringReviewStateRepository(store),
            store,
        )
        var failuresLeft = 1
        val repository = OfflineFirstApplicationRepository(
            jobApplicationDao = FakeJobApplicationDao(),
            clock = fixedClock,
            cleanup = ApplicationCleanup { id ->
                if (failuresLeft-- > 0) throw IllegalStateException("disk full")
                storedCleanup.clearFor(id)
            },
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        repository.upsertApplication(testApplication)
        store.write("prep.plan.${testApplication.id}", "plan")

        val failed = runCatching { repository.deleteApplication(testApplication.id) }

        assertThat(failed.isFailure).isTrue()
        assertThat(repository.observeApplications().first().map { it.id }).containsExactly(testApplication.id)

        val retry = runCatching { repository.deleteApplication(testApplication.id) }

        assertThat(retry.isSuccess).isTrue()
        assertThat(repository.observeApplications().first()).isEmpty()
        assertThat(store.read("prep.plan.${testApplication.id}")).isNull()
    }
}
