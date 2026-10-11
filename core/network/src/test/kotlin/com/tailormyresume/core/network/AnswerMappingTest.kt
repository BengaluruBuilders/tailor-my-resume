package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.network.dto.AnswerChoice
import com.tailormyresume.core.network.mapper.toAnswerDto
import org.junit.Test

class AnswerMappingTest {
    private val requirement = JobRequirement("req-4", "Presents", RequirementType.SOFT_SKILL, RequirementPriority.MUST_HAVE, emptyList())
    private val job = JobDescription("Analyst", "Northwind", "raw", listOf(requirement))

    @Test
    fun everyKnownChoiceMapsAndABlankDetailIsNull() {
        AnswerChoice.entries.forEach { choice ->
            val dto = checkNotNull(QuickAnswer("req-4", choice.name, "   ").toAnswerDto(job))
            assertThat(dto.choice).isEqualTo(choice)
            assertThat(dto.detail).isNull()
        }
    }

    @Test
    fun unknownChoiceOrUnknownRequirementGivesNoAnswer() {
        assertThat(QuickAnswer("req-4", "MAYBE", "x").toAnswerDto(job)).isNull()
        assertThat(QuickAnswer("req-99", "YES_REGULARLY", "x").toAnswerDto(job)).isNull()
    }

    @Test
    fun detailIsCappedAt400CharactersWithoutSplittingASurrogatePair() {
        val emoji = "😀"
        val detail = "a".repeat(399) + emoji

        val capped = checkNotNull(QuickAnswer("req-4", "A_FEW_TIMES", detail).toAnswerDto(job)?.detail)

        assertThat(capped).isEqualTo("a".repeat(399))
        assertThat(QuickAnswer("req-4", "A_FEW_TIMES", "b".repeat(900)).toAnswerDto(job)?.detail).hasLength(400)
        assertThat(QuickAnswer("req-4", "A_FEW_TIMES", "  kept  ").toAnswerDto(job)?.detail).isEqualTo("kept")
    }
}
