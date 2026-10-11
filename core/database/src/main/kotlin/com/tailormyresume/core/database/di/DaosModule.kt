package com.tailormyresume.core.database.di

import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.dao.CreditLedgerDao
import com.tailormyresume.core.database.dao.JobApplicationDao
import com.tailormyresume.core.database.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesProfileDao(
        database: TmrDatabase,
    ): ProfileDao = database.profileDao()

    @Provides
    fun providesJobApplicationDao(
        database: TmrDatabase,
    ): JobApplicationDao = database.jobApplicationDao()

    @Provides
    fun providesCreditLedgerDao(
        database: TmrDatabase,
    ): CreditLedgerDao = database.creditLedgerDao()
}
