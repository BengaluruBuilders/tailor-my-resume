package com.tailormyresume.app.account

import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

internal fun MockWebServer.api(): TailorMyResumeApi = tailormyresumeApi(
    TailorMyResumeApiConfig(url("/").toString().trimEnd('/')),
    tailormyresumeOkHttpClient(object : IdTokenProvider {
        override fun idToken(forceRefresh: Boolean) = "token"
    }),
    tailormyresumeJson(),
)

internal fun jsonResponse(code: Int, body: String) =
    MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

internal fun errorResponse(code: Int, errorCode: String) =
    jsonResponse(code, """{"error":{"code":"$errorCode","message":"m"}}""")
