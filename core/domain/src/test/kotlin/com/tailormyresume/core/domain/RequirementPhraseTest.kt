package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RequirementPhraseTest {

    @Test
    fun of_removesTrailingSentencePunctuation() {
        assertThat(RequirementPhrase.of("  experience 0 to 2 years.  ")).isEqualTo("experience 0 to 2 years")
        assertThat(RequirementPhrase.of("SQL for reporting;")).isEqualTo("SQL for reporting")
    }

    @Test
    fun of_removesTheLeadingPriorityMarkerInAnyCase() {
        assertThat(RequirementPhrase.of("Must have SQL for querying and joining datasets.."))
            .isEqualTo("SQL for querying and joining datasets")
        assertThat(RequirementPhrase.of("Nice to have Agile and JIRA.")).isEqualTo("Agile and JIRA")
        assertThat(RequirementPhrase.of("GOOD-TO-HAVE: Tableau")).isEqualTo("Tableau")
        assertThat(RequirementPhrase.of("must have - Python")).isEqualTo("Python")
    }

    @Test
    fun of_keepsAMarkerWordInsideTheText() {
        assertThat(RequirementPhrase.of("SQL, must have for the role")).isEqualTo("SQL, must have for the role")
        assertThat(RequirementPhrase.of("Musthave tools")).isEqualTo("Musthave tools")
    }

    @Test
    fun of_keepsTheTextWhenOnlyTheMarkerIsLeft() {
        assertThat(RequirementPhrase.of("Must have.")).isEqualTo("Must have")
    }

    @Test
    fun of_returnsEmptyForBlankText() {
        assertThat(RequirementPhrase.of("   ")).isEmpty()
    }
}
