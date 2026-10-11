package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class RemoteResumeTailorAnswerTest {
    private val backend = FakeBackend()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
    private val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(TestMockStateStore(), FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private suspend fun bodyFor(answer: QuickAnswer?): String {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))
        tailor.tailor(candidate, job, gap, "app-1", answer)
        return backend.server.takeRequest().body.readUtf8()
    }

    @Test
    fun answerMapping() = runTest {
        val body = bodyFor(QuickAnswer("req-1", "A_FEW_TIMES", "Presented the variance report to the CFO."))

        assertThat(body).contains(
            """"answer":{"requirementId":"req-1","choice":"A_FEW_TIMES","detail":"Presented the variance report to the CFO."}""",
        )
        assertThat(body).doesNotContain("section")
    }

    @Test
    fun blankDetailIsSentAsNullDetail() = runTest {
        assertThat(bodyFor(QuickAnswer("req-1", "SKIPPED", "   ")))
            .contains(""""answer":{"requirementId":"req-1","choice":"SKIPPED","detail":null}""")
        assertThat(bodyFor(QuickAnswer("req-1", "NOT_YET", "")))
            .contains(""""answer":{"requirementId":"req-1","choice":"NOT_YET","detail":null}""")
    }

    @Test
    fun noAnswerUnknownChoiceOrUnknownRequirementSendNoAnswer() = runTest {
        listOf(null, QuickAnswer("req-1", "MAYBE", "x"), QuickAnswer("req-404", "NOT_YET", "x")).forEach {
            assertThat(bodyFor(it)).doesNotContain("answer")
        }
    }

    @Test
    fun theUseCasePassesTheQuickAnswerToTheTailor() = runTest {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        TailorResumeUseCase(tailor, AllowAllGuard)(
            candidate,
            job,
            gap,
            "app-1",
            quickAnswer = QuickAnswer("req-1", "YES_REGULARLY", "Led SQL reporting."),
        )

        assertThat(backend.server.takeRequest().body.readUtf8()).contains(""""choice":"YES_REGULARLY"""")
    }
}
