package com.tailormyresume.app.network

import com.tailormyresume.app.BuildConfig
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkConfigModule {
    @Provides
    fun apiConfig(): TailorMyResumeApiConfig = TailorMyResumeApiConfig(BuildConfig.TAILORMYRESUME_API_BASE_URL)
}
