package com.tailormyresume.core.testing.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.testing.data.canonicalApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

abstract class ApplicationRepositoryContractTest {

    protected abstract fun createApplicationRepository(): ApplicationRepository

    @Test
    fun aNewAccountHasNoApplications() = runTest {
        val repository = createApplicationRepository()

        assertThat(repository.observeApplications().first()).isEmpty()
    }

    @Test
    fun aSavedApplicationComesBackUnchanged() = runTest {
        val repository = createApplicationRepository()
        val application: JobApplication = canonicalApplication

        repository.upsertApplication(application)

        assertThat(repository.observeApplication(application.id).first()).isEqualTo(application)
        assertThat(repository.observeApplications().first()).containsExactly(application)
    }

    @Test
    fun anUnknownApplicationIsNotFound() = runTest {
        val repository = createApplicationRepository()

        assertThat(repository.observeApplication(UNKNOWN_ID).first()).isNull()
    }

    @Test
    fun savingTheSameIdTwiceReplacesTheApplication() = runTest {
        val repository = createApplicationRepository()
        val application: JobApplication = canonicalApplication
        repository.upsertApplication(application)

        val renamed = application.copy(location = "Pune - Hybrid")
        repository.upsertApplication(renamed)

        assertThat(repository.observeApplications().first()).hasSize(1)
        assertThat(repository.observeApplication(application.id).first()).isEqualTo(renamed)
    }

    @Test
    fun aStatusChangeIsVisibleToTheCaller() = runTest {
        val repository = createApplicationRepository()
        val application: JobApplication = canonicalApplication
        repository.upsertApplication(application)

        repository.updateStatus(application.id, ApplicationStatus.APPLIED)

        assertThat(repository.observeApplication(application.id).first()?.status)
            .isEqualTo(ApplicationStatus.APPLIED)
    }

    @Test
    fun aStatusChangeForAnUnknownIdChangesNothing() = runTest {
        val repository = createApplicationRepository()
        repository.upsertApplication(canonicalApplication)

        repository.updateStatus(UNKNOWN_ID, ApplicationStatus.REJECTED)

        assertThat(repository.observeApplications().first().map { it.id })
            .containsExactly(canonicalApplication.id)
    }

    @Test
    fun deletingRemovesTheApplication() = runTest {
        val repository = createApplicationRepository()
        repository.upsertApplication(canonicalApplication)

        repository.deleteApplication(canonicalApplication.id)

        assertThat(repository.observeApplications().first()).isEmpty()
        assertThat(repository.observeApplication(canonicalApplication.id).first()).isNull()
    }

    @Test
    fun deletingAnUnknownIdChangesNothing() = runTest {
        val repository = createApplicationRepository()
        repository.upsertApplication(canonicalApplication)

        repository.deleteApplication(UNKNOWN_ID)

        assertThat(repository.observeApplications().first()).hasSize(1)
    }

    @Test
    fun theMostRecentlyUpdatedApplicationComesFirst() = runTest {
        val repository = createApplicationRepository()
        val older: JobApplication = canonicalApplication
        val newer = canonicalApplication.copy(
            id = "application-northwind-2",
            updatedAt = older.updatedAt + 1.hours,
        )
        repository.upsertApplication(older)
        repository.upsertApplication(newer)

        val ids = repository.observeApplications().first().map { it.id }

        assertThat(ids).isEqualTo(listOf(newer.id, older.id))
    }

    @Test
    fun roundTripsEveryNewApplicationField() = runTest {
        val repository = createApplicationRepository()
        val application = canonicalApplication.copy(
            location = "Bengaluru - Hybrid",
            appliedOn = Instant.parse("2026-10-08T09:00:00Z"),
            keywordCoverage = ApplicationKeywordCoverage(now = 61, upTo = 92, final = 84),
            exportFileName = "Priya_Northwind_Associate-Analyst.pdf",
            quickAnswer = QuickAnswer(requirementId = "req-presenting", choice = "yes", detail = "Quarterly reviews"),
            changesAcceptedAt = Instant.parse("2026-10-08T09:30:00Z"),
        )

        repository.upsertApplication(application)

        assertThat(repository.observeApplication(application.id).first()).isEqualTo(application)
    }

    private companion object {
        const val UNKNOWN_ID = "application_that_does_not_exist"
    }
}
