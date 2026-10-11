package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.account.FinishPendingAccountWipeUseCase
import com.tailormyresume.core.domain.account.PendingWipeOutcome
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.dto.MeResponse
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class PendingMarkerAccountSwitchTest {
    private val server = MockWebServer().apply { start() }
    private val session = TestSessionRepository()
    private val marker = TestPendingAccountWipe()
    private var wipes = 0
    private val wiper = LocalDataWiper { wipes++ }
    private val deleter = object : ServerAccountDeleter {
        override suspend fun delete() = Result.success(Unit)
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun accountOnlyApi(): TailorMyResumeApi = object : TailorMyResumeApi by server.api() {
        override suspend fun me() = tailormyresumeJson().decodeFromString<MeResponse>(ME_BODY)
    }

    private fun gateway() = RemoteSignInGateway(
        completeConfig,
        ScriptedCredentials(),
        ScriptedFirebase(),
        accountOnlyApi(),
        session,
        server.signOutCleaner(),
        wiper,
        marker,
    )

    private fun finish() = FinishPendingAccountWipeUseCase(
        marker,
        RemoteAccountWipeFinisher(gateway(), wiper, marker),
        deleter,
    )

    @Test
    fun aClosedAccountsLeftoverMarkerNeverWipesTheNextAccount() = runTest {
        marker.current = PendingWipeState.SERVER_CLOSED
        marker.markerUid = "uid-a"

        gateway().signIn()

        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)

        val outcome = finish()()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.NOTHING_PENDING)
        assertThat(wipes).isEqualTo(1)
        assertThat(session.observeAccount().first()?.id).isEqualTo("uid-1")
    }

    @Test
    fun aLeftoverRequestedMarkerForAnotherAccountIsClearedOnSignIn() = runTest {
        marker.current = PendingWipeState.REQUESTED
        marker.markerUid = "uid-a"

        gateway().signIn()

        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun theSameAccountSigningInKeepsTheMarkerAndTheFinishProceeds() = runTest {
        marker.current = PendingWipeState.SERVER_CLOSED
        marker.markerUid = "uid-1"
        val gateway = gateway()

        gateway.signIn()

        assertThat(wipes).isEqualTo(0)
        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)

        val outcome = FinishPendingAccountWipeUseCase(marker, RemoteAccountWipeFinisher(gateway, wiper, marker), deleter)()

        assertThat(outcome).isEqualTo(PendingWipeOutcome.FINISHED)
        assertThat(wipes).isEqualTo(1)
        assertThat(session.observeAccount().first()).isNull()
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun aMarkerWithoutAnAccountBehavesAsBefore() = runTest {
        marker.current = PendingWipeState.SERVER_CLOSED

        gateway().signIn()

        assertThat(wipes).isEqualTo(0)
        assertThat(marker.current).isEqualTo(PendingWipeState.SERVER_CLOSED)
    }

    @Test
    fun signingInReadsTheAccountWithoutANetworkCall() = runTest {
        gateway().signIn()

        assertThat(server.requestCount).isEqualTo(0)
    }
}
