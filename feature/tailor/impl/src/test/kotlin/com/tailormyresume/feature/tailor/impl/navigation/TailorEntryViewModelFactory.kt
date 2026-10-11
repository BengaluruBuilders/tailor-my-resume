package com.tailormyresume.feature.tailor.impl.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import com.tailormyresume.core.testing.repository.TestTailoringReviewStateRepository
import com.tailormyresume.feature.tailor.impl.HandEditBulletUseCase
import com.tailormyresume.feature.tailor.impl.TailorViewModel
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.result.TailoredViewModel
import kotlin.time.Clock

internal fun tailorEntryViewModelFactory(
    applicationRepository: ApplicationRepository,
    profileRepository: ProfileRepository,
    clock: Clock,
) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val reviewState = TestTailoringReviewStateRepository()
        return when (modelClass) {
            TailorViewModel::class.java -> TailorViewModel(
                applicationRepository = applicationRepository,
                profileRepository = profileRepository,
                reviewStateRepository = reviewState,
                contentReportRepository = TestContentReportRepository(),
                clock = clock,
                updateBulletDecision = UpdateBulletDecisionUseCase(applicationRepository, clock),
                handEditBullet = HandEditBulletUseCase(applicationRepository, reviewState, clock),
                applicationId = "app-1",
                scenario = DebugScenario.DEFAULT,
            )
            TailoredViewModel::class.java -> TailoredViewModel(
                applicationRepository = applicationRepository,
                profileRepository = profileRepository,
                assembler = ResumeDocumentAssembler(TestResumeHeadings),
                applicationId = "app-1",
            )
            else -> error("unexpected $modelClass")
        } as T
    }
}
