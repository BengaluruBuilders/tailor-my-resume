package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

enum class StartDestination { SignIn, Applications }

class ObserveStartDestinationUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<StartDestination> =
        combine(
            sessionRepository.observeAccount(),
            sessionRepository.observeOnboardingComplete(),
        ) { account, complete ->
            if (account != null && complete) StartDestination.Applications else StartDestination.SignIn
        }.distinctUntilChanged()
}
