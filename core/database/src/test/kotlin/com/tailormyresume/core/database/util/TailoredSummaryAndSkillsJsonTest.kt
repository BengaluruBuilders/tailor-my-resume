package com.tailormyresume.core.database.util

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.json.GuardrailViolationDto
import com.tailormyresume.core.database.json.TailoredResumeDto
import com.tailormyresume.core.database.json.TailoredSkillsDto
import com.tailormyresume.core.database.json.TailoredTextDto
import com.tailormyresume.core.model.BulletDecision
import org.junit.Test

class TailoredSummaryAndSkillsJsonTest {
    private val converters = JsonConverters()

    @Test
    fun summaryAndSkillsRoundTrip() {
        val resume = TailoredResumeDto(
            bullets = emptyList(),
            summary = TailoredTextDto(
                text = "new",
                original = "old",
                sourceIds = listOf("b-1", "ans-req-1"),
                violations = listOf(GuardrailViolationDto.MissingSource),
                decision = BulletDecision.ACCEPTED,
            ),
            skills = TailoredSkillsDto(listOf("SQL"), listOf("SQL", "Excel"), listOf(GuardrailViolationDto.UnsupportedTerm("Rust"))),
        )

        assertThat(converters.jsonToTailoredResume(converters.tailoredResumeToJson(resume))).isEqualTo(resume)
    }

    @Test
    fun resumeStoredBeforeSummaryAndSkillsExistedDecodes() {
        val stored = """{"bullets":[],"entryIds":["e-1"]}"""

        val decoded = checkNotNull(converters.jsonToTailoredResume(stored))

        assertThat(decoded.summary).isNull()
        assertThat(decoded.skills).isNull()
    }
}
