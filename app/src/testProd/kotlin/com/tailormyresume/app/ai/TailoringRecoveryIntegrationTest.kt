package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.tailoring.TailoringResult
import com.tailormyresume.feature.tailor.impl.tailoring.TailoringRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class TailoringRecoveryIntegrationTest {
    private val backend = FakeBackend()

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun failedLocalStoreRetriesTheSameBackendRequestAndSpendsOnce() = runTest {
        val clock = TestClock()
        val applications = TestApplicationRepository()
        val profiles = TestProfileRepository()
        val credits = TestCreditsRepository()
        val pending = TestMockStateStore()
        val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
        applications.sendApplications(
            listOf(JobApplication("app-1", job, ApplicationStatus.SAVED, gap, null, clock.now(), clock.now())),
        )
        profiles.sendProfile(candidate)
        var failNextWrite = true
        val storage = object : ApplicationRepository by applications {
            override suspend fun upsertApplication(application: JobApplication) {
                if (failNextWrite) {
                    failNextWrite = false
                    error("local storage failed")
                }
                applications.upsertApplication(application)
            }
        }
        val remote = RemoteResumeTailor(backend.api, PendingTailoringIds(pending, FixedIds))
        val runner = TailoringRunner(
            storage,
            profiles,
            credits,
            TailorResumeUseCase(remote, AllowAllGuard),
            clock,
            StandardTestDispatcher(testScheduler),
        )
        repeat(2) { backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT))) }

        val failed = runner("app-1", "run-1")
        assertThat(failed).isInstanceOf(TailoringResult.Failure::class.java)
        assertThat(applications.observeApplication("app-1").first()?.tailoredResume).isNull()
        assertThat(credits.observeLedger().first()).isEmpty()
        assertThat(pending.read("tailoring.request.unfinished.app-1")).isNotNull()

        val retried = runner("app-1", "run-2")
        val repeated = runner("app-1", "run-2")

        assertThat(retried).isEqualTo(TailoringResult.Success)
        assertThat(repeated).isEqualTo(TailoringResult.Success)
        assertThat(applications.observeApplication("app-1").first()?.tailoredResume?.runId).isEqualTo("run-2")
        assertThat(credits.observeLedger().first().filter { it.kind == CreditLedgerKind.SPEND }).hasSize(1)
        assertThat(pending.read("tailoring.request.unfinished.app-1")).isNull()
        val requestIds = List(2) {
            Regex(""""requestId":"([^"]+)"""").find(backend.server.takeRequest().body.readUtf8())?.groupValues?.get(1)
        }
        assertThat(requestIds[0]).isNotNull()
        assertThat(requestIds.distinct()).hasSize(1)
        assertThat(backend.server.requestCount).isEqualTo(2)
    }
}
