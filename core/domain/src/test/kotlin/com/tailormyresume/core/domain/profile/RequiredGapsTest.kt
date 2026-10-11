package com.tailormyresume.core.domain.profile

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.testing.data.PrototypeFixtures
import org.junit.Test

class RequiredGapsTest {

    @Test
    fun freshFixturesYieldOneGapForDataAnalystIntern() {
        assertThat(RequiredGaps.of(PrototypeFixtures.fresh().profile))
            .containsExactly(RequiredGap("exp-tata", "Data Analyst Intern"))
        assertThat(RequiredGaps.of(PrototypeFixtures.returning().profile)).isEmpty()
    }

    @Test
    fun currentEntryIsNotAGap() {
        val profile = profileWith(
            entry(id = "exp-present", endDate = "Present"),
            entry(id = "exp-current", endDate = "Current"),
        )

        assertThat(RequiredGaps.of(profile)).isEmpty()
    }

    @Test
    fun nonExperienceEntryIsNotAGap() {
        val profile = profileWith(
            entry(id = "edu-1", category = EntryCategory.EDUCATION, startDate = "Jun 2018"),
            entry(id = "proj-1", category = EntryCategory.PROJECT, startDate = "Jan 2023"),
            entry(id = "cert-1", category = EntryCategory.CERTIFICATION, startDate = "Mar 2024"),
            entry(id = "ach-1", category = EntryCategory.ACHIEVEMENT, startDate = "Apr 2025"),
        )

        assertThat(RequiredGaps.of(profile)).isEmpty()
    }

    @Test
    fun entryWithEndDateIsNotAGap() {
        assertThat(
            RequiredGaps.of(profileWith(entry(id = "exp-closed", endDate = "May 2022"))),
        ).isEmpty()
        assertThat(
            RequiredGaps.of(profileWith(entry(id = "exp-undated", startDate = "", endDate = ""))),
        ).isEmpty()
        assertThat(
            RequiredGaps.of(profileWith(entry(id = "exp-spaces", endDate = "   "))),
        ).containsExactly(RequiredGap("exp-spaces", "Title"))
    }

    private fun profileWith(vararg entries: ProfileEntry): CandidateProfile =
        PrototypeFixtures.fresh().profile.copy(entries = entries.toList())

    private fun entry(
        id: String,
        category: EntryCategory = EntryCategory.EXPERIENCE,
        startDate: String = "Jun 2021",
        endDate: String = "",
    ) = ProfileEntry(
        id = id,
        category = category,
        title = "Title",
        organization = "Acme",
        startDate = startDate,
        endDate = endDate,
        bullets = emptyList(),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )
}
