package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class RemoteResumeTailorRunIdTest {
    private val backend = FakeBackend()
    private val store = TestMockStateStore()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))

    private fun tailorOverStore() = RemoteResumeTailor(backend.api, PendingTailoringIds(store, FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private fun requestIdOf(body: String) = Regex(""""requestId":"([^"]+)"""").find(body)?.groupValues.orEmpty().last()

    private fun succeed() = backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

    @Test
    fun sameRunIdResendsTheSameRequestIdAfterProcessDeath() = runTest {
        succeed()
        succeed()

        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1")
        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1")

        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(1)
    }

    @Test
    fun aNewRunIdSendsANewRequestId() = runTest {
        succeed()
        succeed()

        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1")
        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-2")

        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(2)
    }

    @Test
    fun aNewRunIdResendsTheRequestIdOfAnAbandonedUnfinishedRun() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        backend.fail(502, "AI_PROVIDER_ERROR")
        succeed()

        runCatching { tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1") }
        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-2")

        val ids = List(3) { backend.server.takeRequest() }.filter { it.method == "POST" }
            .map { requestIdOf(it.body.readUtf8()) }
        assertThat(ids).hasSize(2)
        assertThat(ids.distinct()).hasSize(1)
    }

    @Test
    fun aNewRunIdAfterASucceededRunSendsANewRequestId() = runTest {
        succeed()
        succeed()

        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1")
        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-2")

        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(2)
    }

    @Test
    fun aNewRunIdAfterAFailedRunSendsANewRequestId() = runTest {
        backend.reply(200, tailoringBody("FAILED", ""","failureCode":"INTERRUPTED""""))
        succeed()

        runCatching { tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1") }
        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-2")

        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(2)
    }

    @Test
    fun theUnfinishedPointerStaysUnderThePrefixSignOutWipes() = runTest {
        backend.fail(502, "AI_PROVIDER_ERROR")

        runCatching { tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1") }

        assertThat(store.read("tailoring.request.unfinished.app-1")).isNotNull()
    }

    @Test
    fun aDifferentApplicationNeverAdoptsAnotherApplicationsPointer() = runTest {
        backend.fail(502, "AI_PROVIDER_ERROR")
        succeed()

        runCatching { tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1") }
        tailorOverStore().tailor(candidate, job, gap, "app-2", null, "run-2")

        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(2)
    }

    @Test
    fun successKeepsThePendingIdAndFailedClearsIt() = runTest {
        succeed()
        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1")
        val afterSuccess = store.read("tailoring.request.run-1")
        backend.reply(200, tailoringBody("FAILED", ""","failureCode":"INTERRUPTED""""))

        val failure = runCatching { tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-2") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(AiException::class.java)
        assertThat(afterSuccess).isEqualTo(requestIdOf(backend.server.takeRequest().body.readUtf8()))
        assertThat(store.read("tailoring.request.run-2")).isNull()
    }

    @Test
    fun pendingIdsStayUnderThePrefixSignOutWipes() = runTest {
        succeed()

        tailorOverStore().tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat(store.read("tailoring.request.run-1")).isNotNull()
        assertThat(store.read("tailoring.request.app-1")).isNull()
    }
}
