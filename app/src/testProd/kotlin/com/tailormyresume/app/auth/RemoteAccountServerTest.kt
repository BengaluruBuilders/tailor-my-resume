package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class RemoteAccountServerTest {
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    @Test
    fun deleteCallsDeleteMe() = runTest {
        server.enqueue(jsonResponse(200, """{"deletion":{"status":"done"}}"""))

        val result = RemoteServerAccountDeleter(server.api()).delete()

        assertThat(result.isSuccess).isTrue()
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/v1/me")
    }

    @Test
    fun deleteOfADeletedAccountSucceeds() = runTest {
        server.enqueue(errorResponse(409, "ACCOUNT_DELETED"))

        assertThat(RemoteServerAccountDeleter(server.api()).delete().isSuccess).isTrue()
    }

    @Test
    fun deleteFailsOnAServerError() = runTest {
        server.enqueue(errorResponse(500, "INTERNAL_ERROR"))

        assertThat(RemoteServerAccountDeleter(server.api()).delete().isFailure).isTrue()
    }

    @Test
    fun deleteFailsWhenOffline() = runTest {
        val deleter = RemoteServerAccountDeleter(server.api())
        server.shutdown()

        assertThat(deleter.delete().isFailure).isTrue()
    }
}
