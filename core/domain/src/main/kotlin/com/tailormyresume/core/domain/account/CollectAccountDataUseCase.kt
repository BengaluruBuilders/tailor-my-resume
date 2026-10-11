package com.tailormyresume.core.domain.account

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.PaymentGateway
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Clock

class CollectAccountDataUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val applicationRepository: ApplicationRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val paymentGateway: PaymentGateway,
    private val creditsRepository: CreditsRepository,
    private val resumeSettingsRepository: ResumeSettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): AccountData = AccountData(
        generatedAt = clock.now(),
        account = sessionRepository.observeAccount().first(),
        profile = profileRepository.observeProfile().first(),
        applications = applicationRepository.observeApplications().first(),
        entitlement = paymentGateway.entitlement(),
        purchases = paymentGateway.purchaseHistory(),
        exports = exportHistoryRepository.observeExports().first(),
        creditLedger = creditsRepository.observeLedger().first(),
        resumeSettings = resumeSettingsRepository.observeSettings().first(),
    )
}
