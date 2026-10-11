package com.tailormyresume.core.domain.account

import com.tailormyresume.core.data.mock.LegacyDataPurge
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.DiscardJobDraftsUseCase
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteMyDataUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val sessionRepository: SessionRepository,
    private val discardJobDrafts: DiscardJobDraftsUseCase,
    private val exportedFiles: ExportedFiles,
    private val transientData: TransientDataCleaner,
    private val legacyDataPurge: LegacyDataPurge,
) {
    suspend operator fun invoke(): Result<Unit> = withContext(NonCancellable) {
        try {
            applicationRepository.observeApplications().first().forEach { application ->
                applicationRepository.deleteApplication(application.id)
            }
            profileRepository.clearProfile()
            exportHistoryRepository.clear()
            sessionRepository.observeKeptJobDescription().first()?.let { kept -> discardJobDrafts(kept) }
            sessionRepository.clearKeptJobDescription()
            exportedFiles.deleteAll()
            transientData.clear()
            legacyDataPurge()
            Result.success(Unit)
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }
}
