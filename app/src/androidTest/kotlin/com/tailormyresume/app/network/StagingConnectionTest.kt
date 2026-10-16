package com.tailormyresume.app.network

import android.security.NetworkSecurityPolicy
import androidx.test.platform.app.InstrumentationRegistry
import com.tailormyresume.app.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class StagingConnectionTest {
    @Test
    fun localStagingUsesVerifiedHttpsAndRequiresAuthentication() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("stagingConnectionCheck") == "true")
        assertEquals("com.tailormyresume.app.staging", BuildConfig.APPLICATION_ID)
        assertEquals("tailormy-resume-staging", BuildConfig.TAILORMYRESUME_FIREBASE_PROJECT_ID)
        val baseUrl = BuildConfig.TAILORMYRESUME_API_BASE_URL.toHttpUrl()
        assertTrue(baseUrl.isHttps)
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(baseUrl.host))
        val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
        val health = Request.Builder().url(baseUrl.newBuilder().encodedPath("/health").build()).build()
        client.newCall(health).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("{\"status\":\"ok\"}", response.body.string())
        }
        val account =
            Request.Builder()
                .url(baseUrl.newBuilder().encodedPath("/v1/me").build())
                .header("X-App-Id", "tailormyresume")
                .build()
        client.newCall(account).execute().use { response -> assertEquals(401, response.code) }
    }
}
