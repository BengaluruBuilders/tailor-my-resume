package com.tailormyresume.core.data.model

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import org.junit.Test

class TailoredSummaryAndSkillsMapperTest {
    @Test
    fun summaryAndSkillsSurviveTheDtoRoundTrip() {
        val resume = testTailoredResume.copy(
            summary = TailoredText(
                "new",
                "old",
                listOf("ans-req-1"),
                listOf(GuardrailViolation.UnsupportedNumber("3")),
                BulletDecision.REJECTED,
            ),
            skills = TailoredSkills(listOf("SQL"), listOf("SQL", "Excel"), listOf(GuardrailViolation.UnsupportedTerm("Rust"))),
        )

        assertThat(resume.asDto().asExternalModel()).isEqualTo(resume)
    }
}
