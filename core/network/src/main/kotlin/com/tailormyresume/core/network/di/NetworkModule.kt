package com.tailormyresume.core.network.di

import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.SessionExpiredListener
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = tailormyresumeJson()

    @Provides
    @Singleton
    fun okHttpClient(
        tokens: IdTokenProvider,
        sessionListener: SessionExpiredListener,
    ): OkHttpClient = tailormyresumeOkHttpClient(tokens, sessionListener)

    @Provides
    @Singleton
    fun api(config: TailorMyResumeApiConfig, client: OkHttpClient, json: Json): TailorMyResumeApi =
        tailormyresumeApi(config, client, json)
}
