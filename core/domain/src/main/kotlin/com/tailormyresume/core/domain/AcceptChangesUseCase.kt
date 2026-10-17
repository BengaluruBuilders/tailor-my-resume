package com.tailormyresume.core.domain

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.coverage.withFinalMeasuredOn
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Clock

class AcceptChangesUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(applicationId: String) = DecisionWriteLock.serialised {
        val application = applicationRepository.observeApplication(applicationId).first() ?: return@serialised
        val resume = application.tailoredResume ?: return@serialised
        val accepted = resume.withPendingAccepted()
        if (accepted == resume && application.changesAcceptedAt != null) return@serialised
        val now = clock.now()
        val updated = application.copy(
            tailoredResume = accepted,
            changesAcceptedAt = application.changesAcceptedAt ?: now,
            updatedAt = now,
        )
        val profile = profileRepository.observeProfile().first()
        applicationRepository.upsertApplication(profile?.let { updated.withFinalMeasuredOn(accepted, it) } ?: updated)
    }
}
