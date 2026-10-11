package com.tailormyresume.feature.analysis.impl.di

import com.tailormyresume.feature.analysis.impl.job.AnalysisProgressTicker
import com.tailormyresume.feature.analysis.impl.job.DelayAnalysisProgressTicker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class AnalysisModule {

    @Binds
    abstract fun bindProgressTicker(impl: DelayAnalysisProgressTicker): AnalysisProgressTicker
}
