package com.tailormyresume.app.di

import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.core.domain.OfflineJobImporter
import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.domain.offline.OfflineResumeTextParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AiBindings {
    @Binds
    fun bindJobAnalysisSource(impl: OfflineJobAnalysisSource): JobAnalysisSource

    @Binds
    fun bindResumeTailor(impl: OfflineResumeTailor): ResumeTailor

    @Binds
    fun bindResumeTextParser(impl: OfflineResumeTextParser): ResumeTextParser

    @Binds
    fun bindJobImporter(impl: OfflineJobImporter): JobImporter
}
