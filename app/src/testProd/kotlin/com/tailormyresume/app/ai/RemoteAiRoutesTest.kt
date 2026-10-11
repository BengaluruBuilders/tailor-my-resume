package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class RemoteAiRoutesTest {
    private val backend = FakeBackend()
    private val coverage = KeywordCoverage(1, 2)
    private val matcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) = GapAnalysis(emptyList(), coverage)
    }
    private val source = RemoteJobAnalysisSource(backend.api, matcher)
    private val analysis = JobAnalysisResult(job, GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), coverage))

    @After
    fun tearDown() = backend.shutdown()

    private fun nextBody(): String = backend.server.takeRequest().body.readUtf8()

    @Test
    fun resumeParseSendsOnlyTheTextAndGivesEveryEntryAndBulletItsOwnId() = runBlocking<Unit> {
        backend.reply(200, RESUME_PARSE_RESPONSE)

        val profile = RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("resume text")

        val request = backend.server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/tailormyresume/resume/parse")
        assertThat(request.body.readUtf8()).isEqualTo("""{"text":"resume text"}""")
        assertThat(profile.fullName).isEqualTo(CANDIDATE_NAME)
        val entry = profile.entries.single()
        assertThat(entry.id).isEqualTo("W-01")
        assertThat(entry.bullets.map { it.id }).containsExactly("W-01-b1")
        assertThat(entry.source).isEqualTo(FactSource.IMPORTED)
        assertThat(entry.isConfirmed).isFalse()
    }

    @Test
    fun resumeParseAcceptsTheNullsThatTheBackendSchemaAllows() = runBlocking<Unit> {
        backend.reply(
            200,
            """{"generationId":"g","profile":{"fullName":null,"email":null,"phone":null,"headline":null,"skills":[],
"entries":[{"ref":"e1","category":"PROJECT","title":"Sales dashboard","organization":null,"startDate":null,"endDate":null,
"bullets":[{"ref":"e1b1","text":"Built a dashboard."}]}]},"droppedSensitive":[]}""",
        )

        val profile = RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("resume text")

        assertThat(profile.fullName).isEmpty()
        assertThat(profile.entries.single().organization).isEmpty()
        assertThat(profile.entries.single().endDate).isEmpty()
    }

    @Test
    fun analysisMapsJobMatchesAndKeepsGenerationId() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)

        val result = source.analyse(candidate, "the raw job text")

        assertThat(backend.server.takeRequest().path).isEqualTo("/v1/tailormyresume/analyses")
        assertThat(result.job.rawText).isEqualTo("the raw job text")
        assertThat(result.gap.generationId).isEqualTo("g-analysis")
        assertThat(result.gap.keywordCoverage).isEqualTo(coverage)
        assertThat(result.gap.matches.single().status).isEqualTo(MatchStatus.MET)
        assertThat(result.gap.matches.single().evidenceIds).containsExactly(FACT_ID)
    }

    @Test
    fun evidenceThatIsNotAConfirmedFactBecomesAGap() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        val withoutFacts = candidate.copy(entries = emptyList())

        val match = source.analyse(withoutFacts, "the raw job text").gap.matches.single()

        assertThat(match.status).isEqualTo(MatchStatus.GAP)
        assertThat(match.evidenceIds).isEmpty()
    }

    @Test
    fun sameJobTextAndFactsAreServedFromTheCache() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)

        val first = source.analyse(candidate, "the raw   job text")
        val again = source.analyse(candidate, " the raw job\ntext ")

        assertThat(again).isEqualTo(first)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aChangedFactSetForTheSameJobSendsOneRequest() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        source.analyse(candidate, "the raw job text")

        val closed = candidate.copy(entries = candidate.entries + confirmedEntry("W-02", "W-02-b1", "Wrote SQL reports."))
        source.analyse(closed, "the raw job text")
        source.analyse(closed, "the raw job text")

        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun anUnconfirmedFactDoesNotChangeTheCacheKey() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        source.analyse(candidate, "the raw job text")

        val draft = candidate.copy(entries = candidate.entries + confirmedEntry("W-02").copy(isConfirmed = false))
        source.analyse(draft, "the raw job text")

        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aDifferentJobTextCallsTheServerAgain() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        backend.reply(200, ANALYSIS_RESPONSE)

        source.analyse(candidate, "the raw job text")
        source.analyse(candidate, "another job text")

        assertThat(backend.server.requestCount).isEqualTo(2)
    }

    @Test
    fun aFailedAnalysisIsNotCached() = runBlocking<Unit> {
        backend.fail(502, "AI_PROVIDER_ERROR")
        backend.reply(200, ANALYSIS_RESPONSE)

        runCatching { source.analyse(candidate, "the raw job text") }
        source.analyse(candidate, "the raw job text")

        assertThat(backend.server.requestCount).isEqualTo(2)
    }

    @Test
    fun aRequestNeverCarriesTheNameTheEmailOrThePhone() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        source.analyse(candidate, "the raw job text")
        RemoteResumeTailor(backend.api, PendingTailoringIds(com.tailormyresume.core.testing.mock.TestMockStateStore(), FixedIds))
            .tailor(candidate, job, analysis.gap, "app-1", null, "run-1")

        repeat(2) {
            val body = nextBody()
            assertThat(body).isNotEmpty()
            listOf(CANDIDATE_NAME, "Priya", CANDIDATE_EMAIL, CANDIDATE_PHONE, "98123").forEach {
                assertThat(body).doesNotContain(it)
            }
        }
    }
}
