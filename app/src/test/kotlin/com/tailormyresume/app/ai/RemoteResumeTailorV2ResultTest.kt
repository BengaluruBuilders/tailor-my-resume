package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class RemoteResumeTailorV2ResultTest {
    private val backend = FakeBackend()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1), generationId = "g-analysis")
    private val profile = candidate.copy(summary = "Data analyst with Excel.", skills = listOf("SQL", "Excel"))
    private val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(TestMockStateStore(), FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private fun result(summary: String, skills: String) = tailoringBody(
        "SUCCEEDED",
        """,
"result":{"generationId":"g-tailor","bullets":[{"id":"t-1","entryId":"W-01","sourceIds":["W-01-b1"],
"proposedText":"$FACT_TEXT","editTypes":[],"keywordsUsed":[],"verification":"UNCHANGED"}]$summary$skills}""",
    )

    private val summaryJson =
        ""","summary":{"text":"Data analyst who cleans sales data in Excel.","sourceIds":["W-01-b1"],"verification":"PASSED"}"""

    @Test
    fun summarySkillsMappedAndEmptyOrderedIsNoChange() = runTest {
        backend.reply(202, result(summaryJson, ""","skills":{"ordered":["Excel","SQL"],"added":["Power BI"]}"""))
        backend.reply(202, result(""","summary":null""", ""","skills":{"ordered":[],"added":["Power BI"]}"""))
        backend.reply(202, result("", ""))

        val full = tailor.tailor(profile, job, gap, "app-1", null, "run-1")
        val emptySkills = tailor.tailor(profile, job, gap, "app-1", null, "run-1")
        val missing = tailor.tailor(profile, job, gap, "app-1", null, "run-1")

        val summary = checkNotNull(full.summary)
        val skills = checkNotNull(full.skills)
        assertThat(summary.text).isEqualTo("Data analyst who cleans sales data in Excel.")
        assertThat(summary.original).isEqualTo("Data analyst with Excel.")
        assertThat(skills.skills).containsExactly("Excel", "SQL", "Power BI").inOrder()
        assertThat(skills.original).containsExactly("SQL", "Excel").inOrder()
        assertThat(emptySkills.summary).isNull()
        assertThat(emptySkills.skills).isNull()
        assertThat(missing.summary).isNull()
        assertThat(missing.skills).isNull()
        assertThat(full.bullets.single().generationId).isEqualTo("g-tailor")
    }

    @Test
    fun summaryWithUnresolvableIdFailsClosed() = runTest {
        backend.reply(
            202,
            result(
                ""","summary":{"text":"Data analyst with 10 years of Rust.","sourceIds":["ghost-id"],"verification":"PASSED"}""",
                "",
            ),
        )

        val resume = TailorResumeUseCase(tailor, AllowAllGuard)(profile, job, gap, "app-1", "run-1")

        val summary = checkNotNull(resume.summary)
        assertThat(summary.text).isEqualTo("Data analyst with Excel.")
        assertThat(summary.violations).isNotEmpty()
    }

    @Test
    fun aResolvableSummaryPassesThroughTheDeviceGuard() = runTest {
        backend.reply(202, result(summaryJson, ""))

        val resume = TailorResumeUseCase(tailor, AllowAllGuard)(profile, job, gap, "app-1", "run-1")

        val summary = checkNotNull(resume.summary)
        assertThat(summary.violations).isEmpty()
        assertThat(summary.text).isEqualTo("Data analyst who cleans sales data in Excel.")
    }

    @Test
    fun unknownEditTypeDecodesAndIsDropped() = runTest {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT).replace("\"REWORD\"", "\"SIMPLIFY\",\"REWORD\"")))

        val bullet = tailor.tailor(profile, job, gap, "app-1", null, "run-1").bullets.single()

        assertThat(bullet.editTypes).containsExactly(EditType.REWORD)
    }
}
