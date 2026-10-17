package com.tailormyresume.core.domain.di

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.FabricationGuard
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.IdGenerator
import com.tailormyresume.core.domain.JobDescriptionAnalyzer
import com.tailormyresume.core.domain.LocalAnalyzer
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.offline.OfflineFabricationGuard
import com.tailormyresume.core.domain.offline.OfflineGapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflineAccountWallet
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.domain.offline.UuidIdGenerator
import com.tailormyresume.core.domain.sample.OfflineSampleDataController
import com.tailormyresume.core.domain.sample.SampleDataController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DomainModule {
    @Binds
    @LocalAnalyzer
    abstract fun bindLocalAnalyzer(impl: OfflineJobDescriptionAnalyzer): JobDescriptionAnalyzer

    @Binds
    abstract fun bindGapMatcher(impl: OfflineGapMatcher): GapMatcher

    @Binds
    abstract fun bindFabricationGuard(impl: OfflineFabricationGuard): FabricationGuard

    @Binds
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator

    companion object {
        @Provides
        @Singleton
        fun provideSampleDataController(
            store: MockStateStore,
            sessionRepository: SessionRepository,
            profileRepository: ProfileRepository,
            applicationRepository: ApplicationRepository,
            exportHistoryRepository: ExportHistoryRepository,
            paymentGateway: OfflineAccountWallet,
            analysisSource: OfflineJobAnalysisSource,
            resumeTailor: OfflineResumeTailor,
            fabricationGuard: FabricationGuard,
            clock: Clock,
            firebaseUid: FirebaseUidProvider,
            credits: CreditsRepository,
        ): SampleDataController = OfflineSampleDataController(
            store = store,
            sessionRepository = sessionRepository,
            profileRepository = profileRepository,
            applicationRepository = applicationRepository,
            exportHistoryRepository = exportHistoryRepository,
            paymentGateway = paymentGateway,
            analyzeJob = AnalyzeJobUseCase(analysisSource),
            tailorResume = TailorResumeUseCase(resumeTailor, fabricationGuard),
            clock = clock,
            firebaseUid = firebaseUid,
            credits = credits,
        )
    }
}
