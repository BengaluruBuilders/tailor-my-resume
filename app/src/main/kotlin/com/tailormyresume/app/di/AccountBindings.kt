package com.tailormyresume.app.di

import com.tailormyresume.app.account.RemoteAccountDataExporter
import com.tailormyresume.app.account.RemoteContentReportRepository
import com.tailormyresume.app.auth.RemoteTransientDataCleaner
import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.domain.account.TransientDataCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AccountBindings {
    @Binds
    fun bindAccountDataExporter(impl: RemoteAccountDataExporter): AccountDataExporter

    @Binds
    fun bindContentReportRepository(impl: RemoteContentReportRepository): ContentReportRepository

    @Binds
    fun bindTransientDataCleaner(impl: RemoteTransientDataCleaner): TransientDataCleaner
}
