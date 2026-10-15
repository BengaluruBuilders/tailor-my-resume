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

private const val TWO_REQUIREMENT_RESPONSE = """{"generationId":"g-analysis","job":{"title":"Associate Analyst","company":"Northwind GCC",
"requirements":[{"id":"req-1","text":"Strong SQL","type":"SKILL","priority":"MUST_HAVE","keywords":["sql"]},
{"id":"req-2","text":"Systems Rust","type":"SKILL","priority":"MUST_HAVE","keywords":["rust"]}]},
"matches":[{"requirementId":"req-1","status":"GAP","evidenceIds":[]},{"requirementId":"req-2","status":"GAP","evidenceIds":[]}],
"allowance":{"analysesLeftToday":2,"day":"2026-10-07","resetsAt":"2026-10-07T18:30:00Z"}}"""

class RemoteAnalysisRematchTest {
    private val backend = FakeBackend()
    private val sqlFact = confirmedEntry("W-02", "W-02-b1", "Wrote SQL reports.")
    private val grewProfile = candidate.copy(entries = candidate.entries + sqlFact)
    private val coverage = KeywordCoverage(1, 2)
    private val jobText = "the raw job text"

    private val sqlGapMatcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription): GapAnalysis {
            val knowsSql = profile.entries.any { entry -> entry.bullets.any { it.id == "W-02-b1" } }
            val sqlEvidence = if (knowsSql) listOf("W-02-b1") else emptyList()
            return GapAnalysis(
                listOf(
                    RequirementMatch(job.requirements[0], MatchStatus.PARTIAL, sqlEvidence),
                    RequirementMatch(job.requirements[1], MatchStatus.GAP, emptyList()),
                ),
                coverage,
            )
        }
    }

    private val vanishingEvidenceMatcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription): GapAnalysis {
            val knowsSql = profile.entries.any { entry -> entry.bullets.any { it.id == "W-02-b1" } }
            val status = if (knowsSql) MatchStatus.MET else MatchStatus.GAP
            val evidence = if (knowsSql) listOf("W-02-b1") else emptyList()
            return GapAnalysis(
                listOf(RequirementMatch(job.requirements[0], status, evidence)),
                KeywordCoverage(if (knowsSql) 1 else 0, 1),
            )
        }
    }

    private val flatMatcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) =
            GapAnalysis(
                job.requirements.map { RequirementMatch(it, MatchStatus.GAP, emptyList()) },
                coverage,
            )
    }

    private val source = RemoteJobAnalysisSource(backend.api, sqlGapMatcher)

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun aFactBackedUpgradeSurvivesAnInitialPartialBaseline() = runBlocking<Unit> {
        backend.reply(200, TWO_REQUIREMENT_RESPONSE)

        source.analyse(candidate, jobText)
        val rematched = source.analyse(grewProfile, jobText).gap.matches

        val sql = rematched.first { it.requirement.id == "req-1" }
        assertThat(sql.status.ordinal).isAtMost(MatchStatus.PARTIAL.ordinal)
        assertThat(sql.evidenceIds).contains("W-02-b1")
        assertThat(rematched.first { it.requirement.id == "req-2" }.status).isEqualTo(MatchStatus.GAP)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aRequirementWithNoNewFactEvidenceDoesNotUpgrade() = runBlocking<Unit> {
        backend.reply(200, TWO_REQUIREMENT_RESPONSE)

        source.analyse(candidate, jobText)
        val rust = source.analyse(grewProfile, jobText).gap.matches.first { it.requirement.id == "req-2" }

        assertThat(rust.status).isEqualTo(MatchStatus.GAP)
        assertThat(rust.evidenceIds).isEmpty()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun vanishedServerEvidenceIsReplacedByTheNewDeviceMatchNotBlindlyGap() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        val vanishedSource = RemoteJobAnalysisSource(backend.api, vanishingEvidenceMatcher)
        val onlySql = candidate.copy(entries = listOf(sqlFact))

        vanishedSource.analyse(candidate, jobText)
        val match = vanishedSource.analyse(onlySql, jobText).gap.matches.single()

        assertThat(match.status).isEqualTo(MatchStatus.MET)
        assertThat(match.evidenceIds).contains("W-02-b1")
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun anExhaustedAllowanceAfterTheFirstReplyIsNeverReached() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        backend.fail(429, "ALLOWANCE_EXHAUSTED")
        val flatSource = RemoteJobAnalysisSource(backend.api, flatMatcher)

        flatSource.analyse(candidate, jobText)
        val second = flatSource.analyse(grewProfile, jobText)

        assertThat(backend.server.requestCount).isEqualTo(1)
        assertThat(second.gap.generationId).isEqualTo("g-analysis")
    }

    @Test
    fun aSharedKeywordDeviceMatchCitingOnlyUnchangedEvidenceDoesNotFlipARequirementOnReentry() = runBlocking<Unit> {
        backend.reply(200, TWO_REQUIREMENT_RESPONSE)
        val shared = object : GapMatcher {
            override fun match(profile: CandidateProfile, job: JobDescription) =
                GapAnalysis(
                    job.requirements.map { requirement ->
                        val sql = requirement.id == "req-1"
                        RequirementMatch(requirement, if (sql) MatchStatus.PARTIAL else MatchStatus.GAP, if (sql) listOf("W-01-b1") else emptyList())
                    },
                    coverage,
                )
        }
        val sharedSource = RemoteJobAnalysisSource(backend.api, shared)

        sharedSource.analyse(candidate, jobText)
        val rematched = sharedSource.analyse(grewProfile, jobText).gap.matches.first { it.requirement.id == "req-1" }

        assertThat(rematched.status).isEqualTo(MatchStatus.GAP)
        assertThat(rematched.evidenceIds).isEmpty()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aConfirmedFactBeyondTheSendLimitsThatAlreadyMatchedTheBaselineDoesNotFlipOnReentry() = runBlocking<Unit> {
        backend.reply(200, TWO_REQUIREMENT_RESPONSE)
        val citing = object : GapMatcher {
            override fun match(profile: CandidateProfile, job: JobDescription) =
                GapAnalysis(
                    job.requirements.map { requirement ->
                        val sql = requirement.id == "req-1"
                        RequirementMatch(requirement, if (sql) MatchStatus.PARTIAL else MatchStatus.GAP, if (sql) listOf("W-01-b1") else emptyList())
                    },
                    coverage,
                )
        }
        val citingSource = RemoteJobAnalysisSource(backend.api, citing)
        val unrelated = candidate.copy(entries = candidate.entries + confirmedEntry("W-09", "W-09-b1", "Ran the monthly close."))

        citingSource.analyse(candidate, jobText)
        val rematched = citingSource.analyse(unrelated, jobText).gap.matches.first { it.requirement.id == "req-1" }

        assertThat(rematched.status).isEqualTo(MatchStatus.GAP)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun anOverLimitSkillCitedForAServerGapDoesNotFlipOnReentryOfTheSameProfile() = runBlocking<Unit> {
        backend.reply(200, TWO_REQUIREMENT_RESPONSE)
        val overLimitSkill = "Designing, tuning and maintaining production SQL databases for analytics teams"
        val profile = candidate.copy(skills = candidate.skills + overLimitSkill)
        val citing = object : GapMatcher {
            override fun match(profile: CandidateProfile, job: JobDescription) =
                GapAnalysis(
                    job.requirements.map { requirement ->
                        val sql = requirement.id == "req-1"
                        val evidence = if (sql) listOf("skill:$overLimitSkill") else emptyList()
                        RequirementMatch(requirement, if (sql) MatchStatus.PARTIAL else MatchStatus.GAP, evidence)
                    },
                    coverage,
                )
        }
        val citingSource = RemoteJobAnalysisSource(backend.api, citing)

        citingSource.analyse(profile, jobText)
        val rematched = citingSource.analyse(profile, jobText).gap.matches.first { it.requirement.id == "req-1" }

        assertThat(overLimitSkill.length).isGreaterThan(60)
        assertThat(rematched.status).isEqualTo(MatchStatus.GAP)
        assertThat(rematched.evidenceIds).isEmpty()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }
}
