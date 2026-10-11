package com.tailormyresume.feature.onboarding.impl.di

import com.tailormyresume.feature.onboarding.impl.importresume.ContentResolverResumeFileResolver
import com.tailormyresume.feature.onboarding.impl.importresume.ContentResolverResumeTextSource
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFileResolver
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeTextSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    @Singleton
    abstract fun bindResumeTextSource(impl: ContentResolverResumeTextSource): ResumeTextSource

    @Binds
    @Singleton
    internal abstract fun bindResumeFileResolver(impl: ContentResolverResumeFileResolver): ResumeFileResolver
}
