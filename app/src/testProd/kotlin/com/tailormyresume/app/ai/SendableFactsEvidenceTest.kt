package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class SendableFactsEvidenceTest {
    private val backend = FakeBackend()

    private val answerEntries = (1..14).map { index ->
        confirmedEntry("ans-$index", "ans-$index-b1", "Answer fact $index.").copy(
            source = FactSource.USER_ANSWER,
            bullets = List(15) { EvidenceBullet("ans-$index-b$it", "Answer fact $index.") },
        )
    }
    private val crowdedProfile = candidate.copy(entries = answerEntries + candidate.entries)

    private val matcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) =
            GapAnalysis(listOf(matchOf(MatchStatus.GAP)), KeywordCoverage(0, 1))
    }

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun serverEvidenceIsCheckedAgainstTheSendableFactsEvenWhenAnswerEntriesComeFirst() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)

        val result = RemoteJobAnalysisSource(backend.api, matcher).analyse(crowdedProfile, "the raw job text")

        assertThat(result.gap.matches.single().evidenceIds).containsExactly(FACT_ID)
    }

    @Test
    fun tailoredBulletTakesItsOriginalTextFromTheSendableFacts() = runBlocking<Unit> {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult("Cleaned and checked weekly sales data in Excel.")))
        val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
        val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(TestMockStateStore(), FixedIds))

        val resume = tailor.tailor(crowdedProfile, job, gap, "app-1", null, "run-1")

        assertThat(resume.bullets.single().originalText).isEqualTo(FACT_TEXT)
    }
}
