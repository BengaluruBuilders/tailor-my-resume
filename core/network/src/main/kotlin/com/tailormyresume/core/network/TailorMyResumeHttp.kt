package com.tailormyresume.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

fun tailormyresumeJson(): Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun tailormyresumeOkHttpClient(
    tokens: IdTokenProvider,
    sessionListener: SessionExpiredListener = SessionExpiredListener {},
): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .addInterceptor(AppIdInterceptor())
    .addInterceptor(AuthInterceptor(tokens, sessionListener))
    .build()

fun tailormyresumeApi(config: TailorMyResumeApiConfig, client: OkHttpClient, json: Json): TailorMyResumeApi =
    Retrofit.Builder()
        .baseUrl(config.baseUrl.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TailorMyResumeApi::class.java)

private const val CONNECT_TIMEOUT_SECONDS = 15L
private const val READ_TIMEOUT_SECONDS = 60L
private const val CALL_TIMEOUT_SECONDS = 75L
