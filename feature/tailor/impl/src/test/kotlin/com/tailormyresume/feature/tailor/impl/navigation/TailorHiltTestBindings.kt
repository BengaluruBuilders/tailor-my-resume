package com.tailormyresume.feature.tailor.impl.navigation

import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.StoredContentReportRepository
import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.feature.tailor.impl.document.AndroidResumeHeadings
import com.tailormyresume.feature.tailor.impl.document.ResumeHeadings
import com.tailormyresume.feature.tailor.impl.export.BitmapPdfResumeRenderer
import com.tailormyresume.feature.tailor.impl.export.ExportModule
import com.tailormyresume.feature.tailor.impl.export.ResumePdfRenderer
import com.tailormyresume.feature.tailor.impl.exported.CacheExportedFileStore
import com.tailormyresume.feature.tailor.impl.exported.ExportedFileStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ExportModule::class])
internal abstract class TailorHiltTestBindings {

    @Binds
    abstract fun bindResumePdfRenderer(renderer: BitmapPdfResumeRenderer): ResumePdfRenderer

    @Binds
    abstract fun bindExportedFileStore(store: CacheExportedFileStore): ExportedFileStore

    @Binds
    abstract fun bindResumeHeadings(headings: AndroidResumeHeadings): ResumeHeadings

    @Binds
    abstract fun bindContentReports(impl: StoredContentReportRepository): ContentReportRepository

    companion object {
        @Provides
        fun provideResumeTailor(): ResumeTailor = object : ResumeTailor {
            override suspend fun tailor(
                profile: CandidateProfile,
                job: JobDescription,
                gap: GapAnalysis,
                applicationId: String,
                section: EntryCategory?,
            ): TailoredResume = error("tailoring is not part of this test")
        }
    }
}
