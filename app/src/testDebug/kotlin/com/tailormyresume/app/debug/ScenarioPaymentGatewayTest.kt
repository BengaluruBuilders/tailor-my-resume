package com.tailormyresume.app.debug

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.ForcedPaymentScenario
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScenarioPaymentGatewayTest {

    private val forced = ForcedPaymentScenario()
    private val remote = TestPaymentGateway().withFreeCredits(REMOTE_CREDITS)
    private val offline = TestPaymentGateway(forced = forced).withFreeCredits(OFFLINE_CREDITS)
    private val subject = ScenarioPaymentGateway(remote, offline, forced)

    @Test
    fun withoutAForcedScenarioTheRemoteGatewayAnswers() = runTest {
        assertThat(subject.entitlement().freeCredits).isEqualTo(REMOTE_CREDITS)
    }

    @Test
    fun aForcedScenarioRoutesCallsToTheOfflineGateway() = runTest {
        forced.scenario = DebugScenario.FAILED

        assertThat(subject.entitlement().freeCredits).isEqualTo(OFFLINE_CREDITS)
    }

    @Test
    fun observersFollowTheScenarioAsItChanges() = runTest {
        val seen = mutableListOf<Int>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) {
            subject.observeEntitlement().collect { seen += it.freeCredits }
        }

        forced.scenario = DebugScenario.FAILED
        forced.scenario = DebugScenario.DEFAULT
        collecting.cancel()

        assertThat(seen).containsExactly(REMOTE_CREDITS, OFFLINE_CREDITS, REMOTE_CREDITS).inOrder()
    }

    private companion object {
        const val REMOTE_CREDITS = 5
        const val OFFLINE_CREDITS = 2
    }
}
