package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickQuestion
import com.tailormyresume.core.model.RequirementMatch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

private fun analysisBody(status: String, evidence: String, reason: String?, question: String) =
    """{"generationId":"g-analysis","job":{"title":"Associate Analyst","company":"Northwind GCC","location":"Bengaluru · Hybrid",
"requirements":[{"id":"req-1","text":"Presents to senior stakeholders","type":"SOFT_SKILL","priority":"MUST_HAVE","keywords":["stakeholders"]}]},
"matches":[{"requirementId":"req-1","status":"$status","evidenceIds":[$evidence]${reason?.let { ""","reason":"$it"""" }.orEmpty()}}],
"question":$question,
"allowance":{"analysesLeftToday":2,"day":"2026-10-07","resetsAt":"2026-10-07T18:30:00Z"}}"""

private const val QUESTION = """{"requirementId":"req-1","text":"Have you presented to senior stakeholders?","why":"The role asks for this.",
"options":["YES_REGULARLY","A_FEW_TIMES","NOT_YET"]}"""

class RemoteAnalysisV2Test {
    private val backend = FakeBackend()
    private val coverage = KeywordCoverage(0, 1)
    private val matcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) =
            GapAnalysis(job.requirements.map { RequirementMatch(it, MatchStatus.GAP, emptyList()) }, coverage)
    }
    private val source = RemoteJobAnalysisSource(backend.api, matcher)

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun mapsLocationReasonAndQuestion() = runBlocking<Unit> {
        backend.reply(200, analysisBody("GAP", "", "Your resume does not make this clear.", QUESTION))

        val result = source.analyse(candidate, "the raw job text")

        assertThat(result.job.location).isEqualTo("Bengaluru · Hybrid")
        assertThat(result.gap.matches.single().reason).isEqualTo("Your resume does not make this clear.")
        assertThat(result.gap.question)
            .isEqualTo(QuickQuestion("req-1", "Have you presented to senior stakeholders?", "The role asks for this."))
        assertThat(result.gap.generationId).isEqualTo("g-analysis")
    }

    @Test
    fun nullQuestionStaysNull() = runBlocking<Unit> {
        backend.reply(200, analysisBody("GAP", "", null, "null"))

        assertThat(source.analyse(candidate, "the raw job text").gap.question).isNull()
    }

    @Test
    fun questionDroppedWhenRequirementMetOrUnknown() = runBlocking<Unit> {
        backend.reply(200, analysisBody("MET", """"$FACT_ID"""", "Shown in your role.", QUESTION))
        backend.reply(
            200,
            analysisBody("GAP", "", null, QUESTION.replace("req-1", "req-77")),
        )

        val met = source.analyse(candidate, "the raw job text")
        val unknown = source.analyse(candidate, "another raw job text")

        assertThat(met.gap.matches.single().status).isEqualTo(MatchStatus.MET)
        assertThat(met.gap.question).isNull()
        assertThat(unknown.gap.question).isNull()
    }

    @Test
    fun reasonClearedWhenDeviceChangesStatus() = runBlocking<Unit> {
        backend.reply(200, analysisBody("MET", """"ghost-fact"""", "Shown in your role.", QUESTION))

        val result = source.analyse(candidate, "the raw job text")

        val match = result.gap.matches.single()
        assertThat(match.status).isEqualTo(MatchStatus.GAP)
        assertThat(match.reason).isNull()
        assertThat(result.gap.question).isNotNull()
    }

    @Test
    fun reasonKeptWhenStatusIsUnchanged() = runBlocking<Unit> {
        backend.reply(200, analysisBody("MET", """"$FACT_ID"""", "Shown in your role.", "null"))

        assertThat(source.analyse(candidate, "the raw job text").gap.matches.single().reason)
            .isEqualTo("Shown in your role.")
    }

    @Test
    fun cachedRematchClearsReasonAndQuestionWhenTheProfileUpgradesTheMatch() = runBlocking<Unit> {
        backend.reply(200, analysisBody("GAP", "", "Your resume does not make this clear.", QUESTION))
        val upgrading = object : GapMatcher {
            override fun match(profile: CandidateProfile, job: JobDescription) = GapAnalysis(
                job.requirements.map {
                    val knows = profile.entries.size > 1
                    RequirementMatch(it, if (knows) MatchStatus.MET else MatchStatus.GAP, if (knows) listOf("W-02-b1") else emptyList())
                },
                coverage,
            )
        }
        val upgradingSource = RemoteJobAnalysisSource(backend.api, upgrading)
        upgradingSource.analyse(candidate, "the raw job text")

        val grown = candidate.copy(entries = candidate.entries + confirmedEntry("W-02", "W-02-b1", "Presented to the CFO."))
        val rematched = upgradingSource.analyse(grown, "the raw job text").gap

        assertThat(rematched.matches.single().status).isEqualTo(MatchStatus.MET)
        assertThat(rematched.matches.single().reason).isNull()
        assertThat(rematched.question).isNull()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun cachedRematchDropsTheReasonWhenSomeCitedEvidenceIsNoLongerConfirmed() = runBlocking<Unit> {
        val grown = candidate.copy(entries = candidate.entries + confirmedEntry("W-02", "W-02-b1", "Presented to the CFO."))
        backend.reply(200, analysisBody("MET", """"$FACT_ID","W-02-b1"""", "Presented to the CFO.", "null"))

        val first = source.analyse(grown, "the raw job text").gap.matches.single()
        val second = source.analyse(candidate, "the raw job text").gap.matches.single()

        assertThat(first.reason).isEqualTo("Presented to the CFO.")
        assertThat(second.status).isEqualTo(MatchStatus.MET)
        assertThat(second.evidenceIds).containsExactly(FACT_ID)
        assertThat(second.reason).isNull()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun cachedRematchDropsTheReasonWhenAllCitedEvidenceIsNoLongerConfirmed() = runBlocking<Unit> {
        backend.reply(200, analysisBody("MET", """"$FACT_ID"""", "Shown in your role.", "null"))

        source.analyse(candidate, "the raw job text")
        val second = source.analyse(candidate.copy(entries = emptyList()), "the raw job text").gap.matches.single()

        assertThat(second.status).isEqualTo(MatchStatus.GAP)
        assertThat(second.reason).isNull()
    }

    @Test
    fun notAJobPostIsItsOwnFailureNotCachedAndNotRetried() = runBlocking<Unit> {
        backend.fail(422, "NOT_A_JOB_POST")
        backend.fail(422, "NOT_A_JOB_POST")

        val first = runCatching { source.analyse(candidate, "hello there") }.exceptionOrNull() as AiException
        val second = runCatching { source.analyse(candidate, "hello there") }.exceptionOrNull() as AiException

        assertThat(first.failure).isEqualTo(AiFailure.NotAJobPost)
        assertThat(second.failure).isEqualTo(AiFailure.NotAJobPost)
        assertThat(backend.server.requestCount).isEqualTo(2)
    }
}
