package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.FinishPendingAccountWipeUseCase
import com.tailormyresume.core.domain.account.PendingWipeOutcome
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.SessionExpiredException
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class RequestedMarkerProbeTest {
    private val server = MockWebServer().apply { start() }
    private var wipes = 0
    private val finisher = AccountWipeFinisher { wipes++ }
    private val marker = TestPendingAccountWipe(PendingWipeState.REQUESTED, markerUid = "uid-a")

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun deleterWhoseTokenRefreshFails(expiry: SessionExpiredException) = RemoteServerAccountDeleter(
        tailormyresumeApi(
            TailorMyResumeApiConfig(server.url("/").toString().trimEnd('/')),
            tailormyresumeOkHttpClient(
                object : IdTokenProvider {
                    override fun idToken(forceRefresh: Boolean): String? = throw expiry
                },
            ),
            tailormyresumeJson(),
        ),
    )

    @Test
    fun aDeletedFirebaseUserMeansTheAccountIsClosedAndTheDataIsWiped() = runTest {
        val deleter = deleterWhoseTokenRefreshFails(SessionExpiredException(accountGone = true))

        val outcome = FinishPendingAccountWipeUseCase(marker, finisher, deleter)()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.FINISHED)
        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun aDisabledFirebaseUserKeepsTheDataAndTheMarker() = runTest {
        val deleter = deleterWhoseTokenRefreshFails(SessionExpiredException())

        val outcome = FinishPendingAccountWipeUseCase(marker, finisher, deleter)()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.STILL_PENDING)
        assertThat(wipes).isEqualTo(0)
        assertThat(marker.current).isEqualTo(PendingWipeState.REQUESTED)
    }

    @Test
    fun deleteOnAGoneFirebaseUserCountsAsClosed() = runTest {
        val deleter = deleterWhoseTokenRefreshFails(SessionExpiredException(accountGone = true))

        assertThat(deleter.delete().isSuccess).isTrue()
    }
}
