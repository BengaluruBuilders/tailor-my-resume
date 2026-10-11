package com.tailormyresume.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

internal class AppIdInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("X-App-Id", "tailormyresume").build())
}

internal class AuthInterceptor(
    private val tokens: IdTokenProvider,
    private val sessionListener: SessionExpiredListener,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = try {
            tokens.idToken(forceRefresh = false)
        } catch (expired: SessionExpiredException) {
            sessionListener.onSessionExpired(expired.accountGone)
            return unauthorised(chain.request(), expired)
        } ?: return chain.proceed(chain.request())
        val first = chain.proceed(chain.request().withBearer(token))
        if (first.code != 401) return first
        val fresh = try {
            tokens.idToken(forceRefresh = true)
        } catch (expired: SessionExpiredException) {
            sessionListener.onSessionExpired(expired.accountGone)
            if (expired.accountGone) {
                first.close()
                return unauthorised(chain.request(), expired)
            }
            null
        } catch (failure: IOException) {
            first.close()
            throw failure
        } ?: return first
        first.close()
        val second = chain.proceed(chain.request().withBearer(fresh))
        if (second.code == 401) sessionListener.onSessionExpired(false)
        return second
    }

    private fun unauthorised(request: Request, expired: SessionExpiredException): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Session expired")
        .header("Content-Type", "application/json")
        .body((if (expired.accountGone) GONE_BODY else EXPIRED_BODY).toResponseBody("application/json".toMediaType()))
        .build()

    private companion object {
        const val EXPIRED_BODY = """{"error":{"code":"UNAUTHENTICATED","message":"Session expired"}}"""
        const val GONE_BODY = """{"error":{"code":"ACCOUNT_DELETED","message":"Session expired"}}"""
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}

fun interface SessionExpiredListener {
    fun onSessionExpired(accountGone: Boolean)
}
