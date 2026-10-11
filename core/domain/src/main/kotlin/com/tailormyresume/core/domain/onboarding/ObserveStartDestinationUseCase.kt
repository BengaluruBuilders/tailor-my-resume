package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

enum class StartDestination { SignIn, Upload, Applications }

class ObserveStartDestinationUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<StartDestination> =
        combine(
            sessionRepository.observeAccount(),
            sessionRepository.observeOnboardingComplete(),
            profileRepository.observeProfile(),
        ) { account, complete, profile ->
            when {
                account == null -> StartDestination.SignIn
                complete || profile?.reviewedAt != null -> StartDestination.Applications
                else -> StartDestination.Upload
            }
        }.distinctUntilChanged()
}
