package com.tailormyresume.core.domain

import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.model.KeptJobDescription
import javax.inject.Inject

class DiscardJobDraftsUseCase @Inject constructor(
    private val contentReportRepository: ContentReportRepository,
) {
    suspend operator fun invoke(job: KeptJobDescription) {
        contentReportRepository.clearFor(job.draftKey)
    }
}
