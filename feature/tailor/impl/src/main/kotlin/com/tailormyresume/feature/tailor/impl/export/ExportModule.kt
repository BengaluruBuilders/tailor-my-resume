package com.tailormyresume.feature.tailor.impl.export

import com.tailormyresume.feature.tailor.impl.document.AndroidResumeHeadings
import com.tailormyresume.feature.tailor.impl.document.ResumeHeadings
import com.tailormyresume.feature.tailor.impl.exported.CacheExportedFileStore
import com.tailormyresume.feature.tailor.impl.exported.ExportedFileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ExportModule {

    @Binds
    fun bindResumePdfRenderer(renderer: AndroidPdfResumeRenderer): ResumePdfRenderer

    @Binds
    fun bindExportedFileStore(store: CacheExportedFileStore): ExportedFileStore

    @Binds
    fun bindResumeHeadings(headings: AndroidResumeHeadings): ResumeHeadings
}
