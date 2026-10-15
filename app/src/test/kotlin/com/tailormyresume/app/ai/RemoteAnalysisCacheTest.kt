package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class RemoteAnalysisCacheTest {
    private val backend = FakeBackend()
    private val sqlFact = confirmedEntry("W-02", "W-02-b1", "Wrote SQL reports.")
    private val grewProfile = candidate.copy(entries = candidate.entries + sqlFact)
    private val gapCoverage = KeywordCoverage(0, 1)
    private val metCoverage = KeywordCoverage(1, 1)
    private val serverGap = ANALYSIS_RESPONSE
        .replace(""""status":"MET"""", """"status":"GAP"""")
        .replace(""""evidenceIds":["W-01-b1"]""", """"evidenceIds":[]""")

    private val profileDrivenMatcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription): GapAnalysis {
            val knowsSql = profile.entries.any { entry -> entry.bullets.any { it.id == "W-02-b1" } }
            return if (knowsSql) {
                GapAnalysis(listOf(matchOf(MatchStatus.MET, "W-02-b1")), metCoverage)
            } else {
                GapAnalysis(listOf(matchOf(MatchStatus.GAP)), gapCoverage)
            }
        }
    }

    private val constantGapMatcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) =
            GapAnalysis(listOf(matchOf(MatchStatus.GAP)), gapCoverage)
    }

    private val source = RemoteJobAnalysisSource(backend.api, profileDrivenMatcher)
    private val flatSource = RemoteJobAnalysisSource(backend.api, constantGapMatcher)

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun aChangedProfileForTheSameJobSendsOneRequest() = runBlocking<Unit> {
        backend.reply(200, serverGap)

        source.analyse(candidate, "the raw job text")
        source.analyse(grewProfile, "the raw job text")

        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aSavedFactUpgradesTheCachedRequirementOnTheDevice() = runBlocking<Unit> {
        backend.reply(200, serverGap)

        source.analyse(candidate, "the raw job text")
        val cached = source.analyse(grewProfile, "the raw job text")

        val match = cached.gap.matches.single()
        assertThat(match.status).isEqualTo(MatchStatus.MET)
        assertThat(match.evidenceIds).contains("W-02-b1")
        assertThat(cached.gap.keywordCoverage).isEqualTo(metCoverage)
        assertThat(cached.gap.generationId).isEqualTo("g-analysis")
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aRequirementTheDeviceDoesNotImproveKeepsTheServerStatus() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)

        flatSource.analyse(candidate, "the raw job text")
        val cached = flatSource.analyse(grewProfile, "the raw job text")

        val match = cached.gap.matches.single()
        assertThat(match.status).isEqualTo(MatchStatus.MET)
        assertThat(match.evidenceIds).containsExactly(FACT_ID)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun evidenceOfARemovedFactIsDroppedAndTheRequirementBecomesAGap() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        val onlySql = candidate.copy(entries = listOf(sqlFact))

        flatSource.analyse(candidate, "the raw job text")
        val match = flatSource.analyse(onlySql, "the raw job text").gap.matches.single()

        assertThat(match.status).isEqualTo(MatchStatus.GAP)
        assertThat(match.evidenceIds).isEmpty()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun jobTextThatDiffersOnlyInWhitespaceHitsTheCache() = runBlocking<Unit> {
        backend.reply(200, serverGap)

        source.analyse(candidate, "the raw   job text")
        source.analyse(grewProfile, " the raw job\ntext ")

        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun clearDropsTheCache() = runBlocking<Unit> {
        backend.reply(200, serverGap)
        backend.reply(200, serverGap)

        source.analyse(candidate, "the raw job text")
        source.clear()
        source.analyse(candidate, "the raw job text")

        assertThat(backend.server.requestCount).isEqualTo(2)
    }
}
