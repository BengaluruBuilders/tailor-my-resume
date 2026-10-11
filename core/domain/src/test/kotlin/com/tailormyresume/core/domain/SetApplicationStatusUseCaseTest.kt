package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class SetApplicationStatusUseCaseTest {
    private val repository = FakeApplicationRepository()
    private val now = Instant.fromEpochSeconds(2_000_000_000)
    private val earlier = Instant.fromEpochSeconds(1_900_000_000)
    private val useCase = SetApplicationStatusUseCase(repository, FixedClock(now))

    private fun application(status: ApplicationStatus, appliedOn: Instant? = null) = JobApplication(
        id = "app-1",
        job = JobDescription(title = "Analyst", company = "Acme", rawText = "", requirements = emptyList()),
        status = status,
        gapAnalysis = null,
        tailoredResume = null,
        createdAt = earlier,
        updatedAt = earlier,
        appliedOn = appliedOn,
    )

    private suspend fun stored(): JobApplication = checkNotNull(repository.observeApplication("app-1").first())

    @Test
    fun appliedFromSavedSetsStatusAndTodayAndReturnsThePrevious() = runTest {
        repository.upsertApplication(application(ApplicationStatus.SAVED))

        val previous = useCase("app-1", ApplicationStatus.APPLIED)

        assertThat(previous).isEqualTo(StatusChange(ApplicationStatus.SAVED, null))
        assertThat(stored().status).isEqualTo(ApplicationStatus.APPLIED)
        assertThat(stored().appliedOn).isEqualTo(now)
    }

    @Test
    fun everyStatusOtherThanSavedStampsTodayWhenTheDateIsUnset() = runTest {
        listOf(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER, ApplicationStatus.REJECTED)
            .forEach { status ->
                repository.upsertApplication(application(ApplicationStatus.SAVED))

                useCase("app-1", status)

                assertThat(stored().status).isEqualTo(status)
                assertThat(stored().appliedOn).isEqualTo(now)
            }
    }

    @Test
    fun changingBetweenStatusesRestampsToday() = runTest {
        repository.upsertApplication(application(ApplicationStatus.APPLIED, appliedOn = earlier))

        val previous = useCase("app-1", ApplicationStatus.INTERVIEW)

        assertThat(previous).isEqualTo(StatusChange(ApplicationStatus.APPLIED, earlier))
        assertThat(stored().appliedOn).isEqualTo(now)
    }

    @Test
    fun settingTheSameStatusKeepsTheDate() = runTest {
        repository.upsertApplication(application(ApplicationStatus.APPLIED, appliedOn = earlier))

        useCase("app-1", ApplicationStatus.APPLIED)

        assertThat(stored().appliedOn).isEqualTo(earlier)
    }

    @Test
    fun savedClearsTheDate() = runTest {
        repository.upsertApplication(application(ApplicationStatus.INTERVIEW, appliedOn = earlier))

        useCase("app-1", ApplicationStatus.SAVED)

        assertThat(stored().status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(stored().appliedOn).isNull()
    }

    @Test
    fun restoreBringsBackStatusAndDate() = runTest {
        repository.upsertApplication(application(ApplicationStatus.APPLIED, appliedOn = earlier))
        val previous = checkNotNull(useCase("app-1", ApplicationStatus.REJECTED))

        useCase.restore("app-1", previous)

        assertThat(stored().status).isEqualTo(ApplicationStatus.APPLIED)
        assertThat(stored().appliedOn).isEqualTo(earlier)
    }

    @Test
    fun restoreToSavedClearsTheDate() = runTest {
        repository.upsertApplication(application(ApplicationStatus.SAVED))
        val previous = checkNotNull(useCase("app-1", ApplicationStatus.APPLIED))

        useCase.restore("app-1", previous)

        assertThat(stored().status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(stored().appliedOn).isNull()
    }

    @Test
    fun missingApplicationChangesNothing() = runTest {
        assertThat(useCase("missing", ApplicationStatus.APPLIED)).isNull()
        assertThat(repository.observeApplications().first()).isEmpty()
    }

    @Test
    fun theWriteBumpsUpdatedAt() = runTest {
        repository.upsertApplication(application(ApplicationStatus.SAVED))

        useCase("app-1", ApplicationStatus.APPLIED)

        assertThat(stored().updatedAt).isEqualTo(now)
    }
}
