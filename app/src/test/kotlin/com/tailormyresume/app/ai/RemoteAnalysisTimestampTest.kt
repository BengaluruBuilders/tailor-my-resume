package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class RemoteAnalysisTimestampTest {
    private val backend = FakeBackend()
    private val tenTwelve = Instant.parse("2026-10-09T10:12:00Z")
    private var now = tenTwelve

    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private val flatMatcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) = GapAnalysis(
            job.requirements.map { RequirementMatch(it, MatchStatus.GAP, emptyList()) },
            KeywordCoverage(0, 1),
        )
    }

    private val source = RemoteJobAnalysisSource(backend.api, flatMatcher, clock)

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun aRematchOfACachedResultKeepsTheTimeTheServerAnswered() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)

        val first = source.analyse(candidate, "the raw job text")
        now = Instant.parse("2026-10-09T14:30:00Z")
        val second = source.analyse(candidate, "the raw job text")

        assertThat(first.analysedAt).isEqualTo(tenTwelve)
        assertThat(second.analysedAt).isEqualTo(tenTwelve)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }
}
