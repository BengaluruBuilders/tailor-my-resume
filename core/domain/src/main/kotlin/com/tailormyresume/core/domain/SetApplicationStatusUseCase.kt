package com.tailormyresume.core.domain

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

data class StatusChange(val status: ApplicationStatus, val appliedOn: Instant?)

class SetApplicationStatusUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(applicationId: String, status: ApplicationStatus): StatusChange? =
        DecisionWriteLock.serialised {
            val application = applicationRepository.observeApplication(applicationId).first()
                ?: return@serialised null
            val now = clock.now()
            write(application, StatusChange(status, appliedOnFor(application, status, now)), now)
            StatusChange(application.status, application.appliedOn)
        }

    suspend fun restore(applicationId: String, previous: StatusChange) {
        DecisionWriteLock.serialised {
            val application = applicationRepository.observeApplication(applicationId).first()
                ?: return@serialised
            write(application, previous, clock.now())
        }
    }

    private fun appliedOnFor(application: JobApplication, status: ApplicationStatus, now: Instant): Instant? = when {
        status == ApplicationStatus.SAVED -> null
        application.appliedOn == null || application.status != status -> now
        else -> application.appliedOn
    }

    private suspend fun write(application: JobApplication, change: StatusChange, now: Instant) {
        applicationRepository.upsertApplication(
            application.copy(status = change.status, appliedOn = change.appliedOn, updatedAt = now),
        )
    }
}
