package com.tailormyresume.feature.analysis.impl.navigation

import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.NoRemoteLedger
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import com.tailormyresume.core.domain.ImportedJob
import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AnalysisTestBindings {

    @Provides
    fun jobImporter(): JobImporter = object : JobImporter {
        override suspend fun import(url: String): ImportedJob = error("link import is not part of these tests")
    }

    @Provides
    fun jobAnalysisSource(): JobAnalysisSource = object : JobAnalysisSource {
        override suspend fun analyse(profile: CandidateProfile, rawJobText: String): JobAnalysisResult =
            error("analysis is not part of these tests")
    }

    @Provides
    fun contentReportRepository(): ContentReportRepository = TestContentReportRepository()

    @Provides
    fun remoteLedgerSource(): RemoteLedgerSource = NoRemoteLedger()
}
